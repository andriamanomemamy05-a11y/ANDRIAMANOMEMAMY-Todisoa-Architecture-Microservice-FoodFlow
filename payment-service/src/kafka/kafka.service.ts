import {
  Injectable,
  Logger,
  OnModuleDestroy,
  OnModuleInit,
} from '@nestjs/common';
import { Consumer, Kafka, KafkaMessage, Producer } from 'kafkajs';
import { PaymentsService } from '../payments/payments.service';
import { createEvent, EventEnvelope } from '../events/event-envelope';

/** Payload de l'événement OrderCreated reçu depuis order-service (topic amont). */
interface OrderCreatedPayload {
  id: string;
  customerId: string;
  restaurantId: string;
  amount: number;
  description: string;
  status: string;
}

/** Payload de l'événement de paiement que l'on publie (topic aval). */
interface PaymentPayload {
  orderId: string;
  amount: number;
  status: string;
}

const ORDERS_TOPIC = 'orders.events';
const PAYMENTS_TOPIC = 'payments.events';
const ORDERS_DLQ_TOPIC = 'orders.events.DLQ';

// 3 tentatives au total : 1 initiale (count 0) + 2 retries (count 1 et 2).
const MAX_RETRIES = 2;
const RETRY_HEADER = 'x-retry-count';

@Injectable()
export class KafkaService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(KafkaService.name);

  private readonly kafka = new Kafka({
    clientId: 'payment-service',
    // Repli localhost:9092 (listener EXTERNAL) en dev hors Docker ; dans le
    // compose, KAFKA_BROKERS pointe sur kafka:29092 (listener INTERNAL).
    brokers: (process.env.KAFKA_BROKERS ?? 'localhost:9092').split(','),
  });

  private readonly producer: Producer = this.kafka.producer();
  private readonly consumer: Consumer = this.kafka.consumer({
    groupId: 'payment-service',
  });

  constructor(private readonly paymentsService: PaymentsService) {}

  async onModuleInit(): Promise<void> {
    await this.producer.connect();
    await this.consumer.connect();

    // fromBeginning: false -> on ne consomme que les NOUVEAUX messages, on ne
    // rejoue pas l'historique du topic (anciens tests manuels).
    await this.consumer.subscribe({
      topic: ORDERS_TOPIC,
      fromBeginning: false,
    });

    await this.consumer.run({
      eachMessage: async ({ message }) => {
        // kafkajs n'a NI retry NI DLQ intégrés : on encadre chaque message et on
        // gère l'échec à la main (comptage + republication / bascule DLQ). Surtout,
        // on ne laisse PAS l'exception remonter après la bascule, sinon kafkajs
        // re-livrerait le même message en boucle et bloquerait la partition.
        try {
          await this.handleOrderMessage(message.value?.toString());
        } catch (err) {
          await this.handleFailure(message, err);
        }
      },
    });

    this.logger.log(
      `Kafka prêt — consumer group 'payment-service' abonné à ${ORDERS_TOPIC}`,
    );
  }

  async onModuleDestroy(): Promise<void> {
    await this.consumer.disconnect();
    await this.producer.disconnect();
  }

  private async handleOrderMessage(raw: string | undefined): Promise<void> {
    if (!raw) {
      return;
    }

    // On ne PROTÈGE PAS JSON.parse : un message non désérialisable (pas du JSON)
    // doit lever une exception qui remonte à eachMessage -> retry/DLQ. On distingue
    // « ce qu'on ne PEUT pas traiter » (échec) de « ce qu'on ne VEUT pas » (ci-dessous).
    const envelope = JSON.parse(raw) as EventEnvelope<OrderCreatedPayload>;

    // Tolérance au contrat : le topic transporte plusieurs types d'événements ;
    // on ne réagit qu'au nôtre. Un type inconnu est ignoré, pas une erreur.
    if (envelope.eventType !== 'OrderCreated') {
      this.logger.debug(`Ignoré (eventType=${envelope.eventType})`);
      return;
    }

    const order = envelope.payload;
    this.logger.log(
      `Reçu OrderCreated — orderId=${order.id} amount=${order.amount}`,
    );

    // Réutilisation de la logique métier existante (règle amount > 100 -> REFUSED).
    const payment = this.paymentsService.create({
      orderId: order.id,
      amount: order.amount,
    });
    this.logger.log(`Décision paiement — orderId=${order.id} -> ${payment.status}`);

    await this.publishPaymentEvent(envelope.aggregateId, payment.orderId, payment.amount, payment.status);
  }

  /**
   * Gestion d'un message dont le traitement a échoué. On lit le compteur de
   * tentatives dans un header ; tant qu'il reste des essais on republie le
   * message TEL QUEL (même clé) avec le compteur incrémenté, sinon on le bascule
   * en DLQ. C'est le compteur qui empêche la boucle infinie.
   */
  private async handleFailure(message: KafkaMessage, err: unknown): Promise<void> {
    const retryCount = this.readRetryCount(message);
    const key = message.key ?? null;
    const value = message.value; // Buffer d'origine, republié tel quel
    const errorMessage = err instanceof Error ? err.message : String(err);

    if (retryCount < MAX_RETRIES) {
      const next = retryCount + 1;
      this.logger.warn(
        `Traitement échoué (tentative ${retryCount + 1}/${MAX_RETRIES + 1}): ` +
          `${errorMessage} -> republication sur ${ORDERS_TOPIC} (retry #${next})`,
      );
      await this.producer.send({
        topic: ORDERS_TOPIC,
        messages: [{ key, value, headers: { [RETRY_HEADER]: String(next) } }],
      });
    } else {
      this.logger.error(
        `Échec définitif après ${MAX_RETRIES + 1} tentatives -> DLQ ` +
          `(${ORDERS_DLQ_TOPIC}): ${errorMessage}`,
      );
      await this.producer.send({
        topic: ORDERS_DLQ_TOPIC,
        messages: [
          {
            key,
            value,
            headers: {
              [RETRY_HEADER]: String(retryCount),
              'x-error-message': errorMessage,
              'x-original-topic': ORDERS_TOPIC,
            },
          },
        ],
      });
    }
  }

  private readRetryCount(message: KafkaMessage): number {
    const header = message.headers?.[RETRY_HEADER];
    if (!header) {
      return 0;
    }
    const n = Number(header.toString());
    return Number.isNaN(n) ? 0 : n;
  }

  private async publishPaymentEvent(
    key: string,
    orderId: string,
    amount: number,
    status: string,
  ): Promise<void> {
    const eventType =
      status === 'AUTHORIZED' ? 'PaymentAuthorized' : 'PaymentRefused';

    // Clé = aggregateId (l'orderId) : LA MÊME que celle reçue, pour que la
    // réponse de paiement reste dans la même partition que la commande.
    const event = createEvent<PaymentPayload>(eventType, key, {
      orderId,
      amount,
      status,
    });

    await this.producer.send({
      topic: PAYMENTS_TOPIC,
      messages: [{ key, value: JSON.stringify(event) }],
    });

    this.logger.log(
      `Publié ${eventType} sur ${PAYMENTS_TOPIC} — clé=${key}`,
    );
  }
}

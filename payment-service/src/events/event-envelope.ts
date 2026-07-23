import { randomUUID } from 'crypto';

/**
 * Enveloppe partagée de tous les événements FoodFlow (convention polyglotte,
 * répliquée à l'identique côté order-service en Java).
 *
 * - `eventId`     : identifiant unique DU message (base de l'idempotence)
 * - `eventType`   : type de l'événement, ex. "PaymentAuthorized"
 * - `occurredAt`  : date ISO-8601 où le fait s'est produit
 * - `aggregateId` : identifiant de l'agrégat (l'orderId) — aussi la clé Kafka
 * - `payload`     : données métier propres au type d'événement
 */
export interface EventEnvelope<T> {
  eventId: string;
  eventType: string;
  occurredAt: string;
  aggregateId: string;
  payload: T;
}

/**
 * Fabrique un événement en générant automatiquement son `eventId`
 * et son `occurredAt`.
 */
export function createEvent<T>(
  eventType: string,
  aggregateId: string,
  payload: T,
): EventEnvelope<T> {
  return {
    eventId: randomUUID(),
    eventType,
    occurredAt: new Date().toISOString(),
    aggregateId,
    payload,
  };
}

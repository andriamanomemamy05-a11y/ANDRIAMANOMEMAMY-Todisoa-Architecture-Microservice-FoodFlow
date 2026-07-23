package fr.esgi.foodflow.order_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Consomme les événements de paiement (topic aval payments.events) et déclenche
 * la boucle de retour de la saga : PaymentAuthorized -> CONFIRMED,
 * PaymentRefused -> CANCELLED. Le traitement idempotent est délégué au processor.
 */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final ObjectMapper objectMapper;
    private final PaymentEventProcessor processor;

    public PaymentEventListener(ObjectMapper objectMapper, PaymentEventProcessor processor) {
        this.objectMapper = objectMapper;
        this.processor = processor;
    }

    @KafkaListener(topics = "payments.events", groupId = "order-service")
    public void onPaymentEvent(String message) {
        // 1. Désérialiser l'enveloppe avec l'ObjectMapper Jackson 3 (les champs
        //    inconnus comme occurredAt/payload sont ignorés).
        //    On N'AVALE PAS l'erreur : un message non désérialisable (pas du JSON)
        //    lève une exception qui REMONTE hors du listener -> DefaultErrorHandler
        //    (retries puis DLT). Si on l'attrapait, Spring Kafka considérerait le
        //    message traité : ni retry, ni DLT.
        PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);

        // 2. Tolérance au contrat : un JSON VALIDE mais d'un type qu'on ne veut pas
        //    traiter est ignoré silencieusement (pas une erreur). On distingue bien
        //    « ce qu'on ne VEUT pas traiter » (ignore) de « ce qu'on ne PEUT pas
        //    traiter » (échec ci-dessus).
        if (!"PaymentAuthorized".equals(event.eventType()) && !"PaymentRefused".equals(event.eventType())) {
            log.debug("Ignoré (eventType={})", event.eventType());
            return;
        }

        log.info("Reçu {} pour orderId={} (eventId={})",
                event.eventType(), event.aggregateId(), event.eventId());

        // 3. Effet métier idempotent, dans une transaction (voir PaymentEventProcessor).
        processor.process(event.eventId(), event.eventType(), event.aggregateId());
    }

    /** Vue minimale de l'enveloppe reçue : seuls les champs utiles au listener. */
    public record PaymentEvent(UUID eventId, String eventType, UUID aggregateId) { }
}

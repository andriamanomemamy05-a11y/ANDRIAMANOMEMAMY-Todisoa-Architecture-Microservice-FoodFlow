package fr.esgi.foodflow.order_service.event;

import fr.esgi.foodflow.order_service.entity.Commande;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Isole la publication Kafka du reste de l'application : le contrôleur et le
 * listener appellent ce publisher, ils ne parlent jamais directement au broker.
 * Même principe que le PaymentGateway qui isole l'appel HTTP.
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    /** Topic des événements du bounded context Commande. */
    public static final String TOPIC = "orders.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publie l'événement fondateur d'une commande.
     * Clé = orderId (String) → tous les événements d'une même commande vont dans
     * la même partition et restent ordonnés.
     * Valeur = l'enveloppe sérialisée en JSON par l'ObjectMapper de l'application.
     */
    public void publishOrderCreated(Commande commande) {
        EventEnvelope<OrderCreatedPayload> event = EventEnvelope.of(
                "OrderCreated",
                commande.getId(),
                OrderCreatedPayload.from(commande));

        String json = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(TOPIC, commande.getId().toString(), json);
        log.info("Publié OrderCreated pour orderId={}", commande.getId());
    }

    /**
     * Étape de compensation : publie OrderCancelled sur orders.events.
     * Ce n'est pas un simple UPDATE — c'est un événement à part entière de la
     * chorégraphie, auquel d'autres services pourront réagir (remboursement,
     * notification, arrêt de préparation) sans qu'order-service les connaisse.
     */
    public void publishOrderCancelled(Commande commande, String reason) {
        EventEnvelope<OrderCancelledPayload> event = EventEnvelope.of(
                "OrderCancelled",
                commande.getId(),
                OrderCancelledPayload.from(commande, reason));

        String json = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(TOPIC, commande.getId().toString(), json);
        log.info("Publié OrderCancelled (compensation) pour orderId={} raison={}",
                commande.getId(), reason);
    }
}

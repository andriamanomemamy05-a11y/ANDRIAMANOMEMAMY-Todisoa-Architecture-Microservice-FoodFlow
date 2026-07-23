package fr.esgi.foodflow.order_service.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Enveloppe partagée de tous les événements FoodFlow (convention polyglotte,
 * répliquée à l'identique côté payment-service en TypeScript).
 *
 * @param eventId     identifiant unique DU message (base de l'idempotence)
 * @param eventType   type de l'événement, ex. "OrderCreated"
 * @param occurredAt  instant où le fait s'est produit
 * @param aggregateId identifiant de l'agrégat (l'orderId) — aussi la clé Kafka
 * @param payload     données métier propres au type d'événement
 */
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID aggregateId,
        T payload) {

    /**
     * Fabrique un événement en générant automatiquement son {@code eventId}
     * et son {@code occurredAt}.
     */
    public static <T> EventEnvelope<T> of(String eventType, UUID aggregateId, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), eventType, Instant.now(), aggregateId, payload);
    }
}

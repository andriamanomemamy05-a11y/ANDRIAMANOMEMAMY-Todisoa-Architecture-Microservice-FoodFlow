package fr.esgi.foodflow.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Trace des événements déjà consommés, pour l'idempotence.
 * La clé primaire = eventId : c'est la contrainte d'unicité de la base qui
 * garantit qu'un même événement (même eventId) ne sera traité qu'une seule fois,
 * quel que soit le nombre de fois où Kafka le livre (livraison at-least-once).
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    private UUID eventId;

    @Column(nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() { }

    public ProcessedEvent(UUID eventId, Instant processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    public UUID getEventId() { return eventId; }
    public Instant getProcessedAt() { return processedAt; }
}

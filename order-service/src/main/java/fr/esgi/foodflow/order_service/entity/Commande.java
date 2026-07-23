package fr.esgi.foodflow.order_service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "commandes")
public class Commande {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private UUID restaurantId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommandeStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Commande() { }

    public Commande(UUID customerId, UUID restaurantId, BigDecimal amount, String description) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.amount = amount;
        this.description = description;
        this.status = CommandeStatus.CREATED;
        this.createdAt = Instant.now();
    }

    // Transitions métier : c'est l'agrégat qui change son propre statut, pas un
    // setter public. Idempotentes (confirmer une commande déjà confirmée est un
    // no-op) pour rester sûres face à une reconsommation d'événement.
    public void confirmer() {
        this.status = CommandeStatus.CONFIRMED;
    }

    public void annuler() {
        this.status = CommandeStatus.CANCELLED;
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public UUID getRestaurantId() { return restaurantId; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public CommandeStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
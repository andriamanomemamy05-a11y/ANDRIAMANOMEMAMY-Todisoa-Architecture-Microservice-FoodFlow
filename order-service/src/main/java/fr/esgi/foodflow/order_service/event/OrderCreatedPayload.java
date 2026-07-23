package fr.esgi.foodflow.order_service.event;

import fr.esgi.foodflow.order_service.entity.Commande;
import fr.esgi.foodflow.order_service.entity.CommandeStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Données métier transportées dans le payload d'un événement {@code OrderCreated}.
 * C'est le contenu propre à ce type d'événement, distinct de l'enveloppe commune.
 */
public record OrderCreatedPayload(
        UUID id,
        UUID customerId,
        UUID restaurantId,
        BigDecimal amount,
        String description,
        CommandeStatus status) {

    public static OrderCreatedPayload from(Commande commande) {
        return new OrderCreatedPayload(
                commande.getId(),
                commande.getCustomerId(),
                commande.getRestaurantId(),
                commande.getAmount(),
                commande.getDescription(),
                commande.getStatus());
    }
}

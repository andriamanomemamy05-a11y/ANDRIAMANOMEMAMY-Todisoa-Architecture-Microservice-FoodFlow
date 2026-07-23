package fr.esgi.foodflow.order_service.event;

import fr.esgi.foodflow.order_service.entity.Commande;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload de l'événement OrderCancelled (étape de compensation de la saga).
 * Porte la raison de l'annulation pour que les consommateurs futurs
 * (remboursement, notification, arrêt de préparation) sachent pourquoi réagir.
 */
public record OrderCancelledPayload(UUID orderId, BigDecimal amount, String reason) {

    public static OrderCancelledPayload from(Commande commande, String reason) {
        return new OrderCancelledPayload(commande.getId(), commande.getAmount(), reason);
    }
}

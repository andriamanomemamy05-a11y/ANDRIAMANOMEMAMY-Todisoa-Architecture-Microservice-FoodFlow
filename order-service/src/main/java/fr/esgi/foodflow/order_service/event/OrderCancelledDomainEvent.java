package fr.esgi.foodflow.order_service.event;

import fr.esgi.foodflow.order_service.entity.Commande;

/**
 * Événement applicatif INTERNE (Spring), à ne pas confondre avec l'événement
 * Kafka OrderCancelled. Il est émis à l'intérieur de la transaction de
 * compensation ; un listener AFTER_COMMIT le relaie ensuite vers Kafka, de sorte
 * que la publication n'a lieu que si la transaction a bien été committée.
 */
public record OrderCancelledDomainEvent(Commande commande, String reason) {
}

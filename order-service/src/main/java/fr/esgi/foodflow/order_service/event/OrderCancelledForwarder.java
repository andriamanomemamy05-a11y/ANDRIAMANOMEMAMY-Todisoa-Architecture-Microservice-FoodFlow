package fr.esgi.foodflow.order_service.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Relaie l'annulation d'une commande vers Kafka APRÈS le commit de la transaction
 * de compensation.
 *
 * On publie hors transaction : publier DANS la transaction pourrait émettre un
 * OrderCancelled alors qu'un rollback annule la mise à jour du statut — on aurait
 * annoncé au monde une annulation qui n'a pas eu lieu. AFTER_COMMIT ne se déclenche
 * que si la transaction a effectivement été committée.
 *
 * (La double écriture base + Kafka reste faillible si la publication échoue après
 * le commit : c'est le pattern Outbox, hors périmètre ici.)
 */
@Component
public class OrderCancelledForwarder {

    private final OrderEventPublisher publisher;

    public OrderCancelledForwarder(OrderEventPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledDomainEvent event) {
        publisher.publishOrderCancelled(event.commande(), event.reason());
    }
}

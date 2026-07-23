package fr.esgi.foodflow.order_service.event;

import fr.esgi.foodflow.order_service.entity.Commande;
import fr.esgi.foodflow.order_service.entity.ProcessedEvent;
import fr.esgi.foodflow.order_service.repository.CommandeRepository;
import fr.esgi.foodflow.order_service.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Applique l'effet métier d'un événement de paiement, de façon idempotente.
 *
 * La déduplication (insertion de l'eventId) ET la mise à jour du statut de la
 * commande sont dans LA MÊME transaction : si la mise à jour échoue, l'insertion
 * de déduplication est annulée (rollback), sinon on marquerait l'événement
 * "traité" alors qu'il ne l'a pas été — et il ne serait jamais rejoué.
 */
@Service
public class PaymentEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProcessor.class);

    private final ProcessedEventRepository processedEvents;
    private final CommandeRepository commandes;

    public PaymentEventProcessor(ProcessedEventRepository processedEvents, CommandeRepository commandes) {
        this.processedEvents = processedEvents;
        this.commandes = commandes;
    }

    @Transactional
    public void process(UUID eventId, String eventType, UUID orderId) {
        // Idempotence : si l'eventId est déjà en base, on a déjà traité ce message.
        if (processedEvents.existsById(eventId)) {
            log.info("Événement déjà traité, ignoré: {}", eventId);
            return;
        }
        processedEvents.save(new ProcessedEvent(eventId, Instant.now()));

        Commande commande = commandes.findById(orderId).orElse(null);
        if (commande == null) {
            log.warn("Commande introuvable pour orderId={} (eventId={})", orderId, eventId);
            return;
        }

        switch (eventType) {
            case "PaymentAuthorized" -> commande.confirmer();
            case "PaymentRefused" -> commande.annuler();
            default -> {
                return; // filtré en amont ; garde-fou
            }
        }
        commandes.save(commande);

        log.info("Statut appliqué: orderId={} -> {} (eventType={}, eventId={})",
                orderId, commande.getStatus(), eventType, eventId);
    }
}

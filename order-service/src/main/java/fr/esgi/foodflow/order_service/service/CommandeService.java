package fr.esgi.foodflow.order_service.service;

import fr.esgi.foodflow.order_service.dto.CreateCommandeRequest;
import fr.esgi.foodflow.order_service.entity.Commande;
import fr.esgi.foodflow.order_service.event.OrderCreatedPayload;
import fr.esgi.foodflow.order_service.repository.CommandeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Cas d'usage Commande. La création est transactionnelle : la Commande et la ligne
 * Kafka ni au repository pour créer — il délègue ici.
 */
@Service
public class CommandeService {

    private static final Logger log = LoggerFactory.getLogger(CommandeService.class);

    private final CommandeRepository commandes;

    public CommandeService(CommandeRepository commandes) {
        this.commandes = commandes;
    }

    @Transactional
    public Commande creer(CreateCommandeRequest request) {
        Commande commande = new Commande(
                request.customerId(),
                request.restaurantId(),
                request.amount(),
                request.description());
        Commande saved = commandes.save(commande);

        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Commande> findById(UUID id) {
        return commandes.findById(id);
    }
}

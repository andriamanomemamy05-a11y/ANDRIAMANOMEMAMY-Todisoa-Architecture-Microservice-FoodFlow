package fr.esgi.foodflow.order_service.repository;

import fr.esgi.foodflow.order_service.entity.Commande;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CommandeRepository extends JpaRepository<Commande, UUID> {
}
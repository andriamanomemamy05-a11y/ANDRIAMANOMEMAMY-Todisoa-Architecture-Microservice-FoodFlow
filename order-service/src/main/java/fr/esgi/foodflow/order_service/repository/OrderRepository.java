// Interface d'accès aux données
// Spring Data JPA générera automatiquement toutes les requêtes SQL (save, findById, etc.).

package fr.esgi.foodflow.order_service.repository;

import fr.esgi.foodflow.order_service.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
}
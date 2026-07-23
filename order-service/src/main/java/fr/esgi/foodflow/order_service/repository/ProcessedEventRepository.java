package fr.esgi.foodflow.order_service.repository;

import fr.esgi.foodflow.order_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}

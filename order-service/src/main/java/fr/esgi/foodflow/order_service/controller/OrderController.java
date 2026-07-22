// L'API REST
// Expose les endpoints POST /orders (création) et GET /orders/{id} (lecture)
package fr.esgi.foodflow.order_service.controller;

import fr.esgi.foodflow.order_service.dto.UpdateOrderStatusDto;
import fr.esgi.foodflow.order_service.entity.Order;
import fr.esgi.foodflow.order_service.repository.OrderRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import fr.esgi.foodflow.order_service.service.PaymentGateway;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public OrderController(OrderRepository orderRepository, PaymentGateway paymentGateway) {
    this.orderRepository = orderRepository;
    this.paymentGateway = paymentGateway;
}

    // 1. POST /orders : Créer une commande
    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody Order order) {
        Order savedOrder = orderRepository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedOrder);
    }

    // 2. GET /orders : Lister toutes les commandes
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }

    // 3. GET /orders/{id} : Récupérer une commande par son ID
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable UUID id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4. PATCH /orders/{id}/status : Modifier le statut d'une commande
    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusDto updateDto) {
        
        return orderRepository.findById(id).map(order -> {
            order.setStatus(updateDto.getStatus());
            Order updatedOrder = orderRepository.save(order);
            return ResponseEntity.ok(updatedOrder);
        }).orElse(ResponseEntity.notFound().build());
    }

    // 5. DELETE /orders/{id} : Supprimer une commande
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID id) {
        if (!orderRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        orderRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/payment-status")
    public ResponseEntity<String> getOrderPaymentStatus(@PathVariable UUID id) {
        String status = paymentGateway.paymentStatus(id);
        return ResponseEntity.ok(status);
    }
}
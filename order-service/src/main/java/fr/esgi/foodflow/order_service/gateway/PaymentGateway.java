package fr.esgi.foodflow.order_service.gateway;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
public class PaymentGateway {

    private final RestClient paymentClient;

    public PaymentGateway(RestClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    @CircuitBreaker(name = "payment", fallbackMethod = "statusFallback")
    public String paymentStatus(UUID orderId) {
        return paymentClient.get()
                .uri("/payments/by-order/{id}/status", orderId)
                .retrieve()
                .body(String.class);
    }

    String statusFallback(UUID orderId, Throwable t) {
        return "UNKNOWN"; // décision métier : l'état est inconnu, pas faux
    }
}
package fr.esgi.foodflow.order_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateCommandeRequest(
        @NotNull UUID customerId,
        @NotNull UUID restaurantId,
        @NotNull @Positive BigDecimal amount,
        String description
) { }
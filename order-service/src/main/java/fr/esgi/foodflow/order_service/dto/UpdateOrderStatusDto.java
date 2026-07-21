// Un DTO pour la mise à jour de statut

package fr.esgi.foodflow.order_service.dto;

import fr.esgi.foodflow.order_service.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateOrderStatusDto {

    @NotNull(message = "Le statut est obligatoire")
    private OrderStatus status;

    public UpdateOrderStatusDto() {}

    public UpdateOrderStatusDto(OrderStatus status) {
        this.status = status;
    }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}
// L'Enum du cycle de vie
// Ce fichier définit tous les états possibles d'une commande selon ton Event Storming.

package fr.esgi.foodflow.order_service.model;

public enum OrderStatus {
    CREATED,
    PAID,
    PREPARING,
    DELIVERED,
    CANCELLED,
    REFUSED
}
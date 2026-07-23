package fr.esgi.foodflow.order_service.controller;

import fr.esgi.foodflow.order_service.dto.CreateCommandeRequest;
import fr.esgi.foodflow.order_service.entity.Commande;
import fr.esgi.foodflow.order_service.gateway.PaymentGateway;
import fr.esgi.foodflow.order_service.service.CommandeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class CommandeController {

    private final CommandeService commandeService;
    private final PaymentGateway paymentGateway;

    public CommandeController(CommandeService commandeService, PaymentGateway paymentGateway) {
        this.commandeService = commandeService;
        this.paymentGateway = paymentGateway;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Commande create(@Valid @RequestBody CreateCommandeRequest request) {
        return commandeService.creer(request);
    }

    @GetMapping("/{id}")
    public Commande get(@PathVariable UUID id) {
        return commandeService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{id}/payment-status")
    public String paymentStatus(@PathVariable UUID id) {
        return paymentGateway.paymentStatus(id);
    }
}

# FoodFlow - Architecture Microservices

**Étudiant :** ANDRIAMANOMEMAMY Todisoa Bien Aimée
**Module :** Architecture Microservices (4ESGI-AL)
**Sujet :** FoodFlow - Commande & Livraison de repas

---

## User Stories Minimales

- **Passage de commande :** Je passe une commande ; si le paiement est accepté, elle part en préparation.
- **Gestion des échecs (Saga) :** Si le paiement est refusé, la commande est annulée automatiquement (compensation).
- **Suivi :** Je peux consulter l'état courant de ma commande.

---

## Capacités Métier (Préparation Séance 2)

1. **Passer** une commande
2. **Autoriser / Payeur** un règlement de commande
3. **Refuser** un paiement
4. **Annuler** une commande
5. **Préparer** un repas (Restaurant)
6. **Livrer** une commande (Livreur)
7. **Suivre** le statut d'une commande
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

## TP3 - Résilience et Circuit Breaker

Nous avons configuré Resilience4j sur `order-service` pour protéger les appels vers `payment-service`.

### Séquence de test observée :
1. **État CLOSED** : `payment-service` est en ligne, l'appel HTTP inter-services renvoie le statut réel du paiement.
2. **Coupure du service** : Arret de `payment-service` (`docker compose stop payment-service`).
3. **Passage à OPEN** : Après 5 échecs consécutifs, le Circuit Breaker bascule en état `OPEN`. Les appels suivants renvoient immédiatement le fallback `"UNKNOWN"` sans attendre le timeout HTTP.
4. **Passage à HALF-OPEN & CLOSED** : Redémarrage de `payment-service`. Après 10s d'attente, les requêtes de test passent, l'état bascule en `HALF-OPEN` puis se referme définitivement en `CLOSED`.
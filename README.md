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

# FoodFlow - TP4 Kafka

## Questions de synthèse TP4

- **Pourquoi 3 partitions ?** Pour permettre le traitement parallèle. Un Consumer Group peut ainsi avoir jusqu'à 3 instances travaillant en même temps (une par partition). --> Nous configurons 3 partitions par topic pour permettre le parallélisme. Au sein d'un même Consumer Group, chaque partition est attribuée à une seule instance. Avec 3 partitions, nous pouvons monter jusqu'à 3 instances de service qui travaillent simultanément sans se marcher sur les pieds.


- **Pourquoi Replication Factor (RF) = 1 localement ?** Car nous n'avons qu'un seul Broker Kafka dans Docker local. En production, un $RF >= 3$ est obligatoire pour éviter de perdre des données si un serveur tombe. --> Un $RF = 1$ signifie qu'il n'y a qu'une seule copie des données, stockée sur notre broker local.  En production : Un $RF = 1$ est inacceptable car en cas de panne du broker, les données sont indisponibles ou perdues (Single Point of Failure). En prod, on utilise un $RF >= 3$.


- **Définitions :**
  - `CURRENT-OFFSET` : Dernier message lu et validé par le consommateur. --> Le numéro de séquence (offset) du dernier message lu et commité par l'instance
  - `LOG-END-OFFSET` : Dernier message produit dans Kafka. --> Le numéro de séquence du dernier message écrit dans le topic Kafka (la fin du journal)
  - `LAG` : Différence entre les deux (C'est l'indicateur principal du retard de traitement de nos services.).
- **Rebalance observé :** Lors de l'arrêt d'un consommateur dans un groupe, Kafka réaffecte automatiquement ses partitions aux consommateurs restants sans perte de message.
# Découpage Métier - FoodFlow
## Consigne de mon Event Storming light en suivant les 4 passes de modélisation.

## 1. Événements (Passe 1)

Liste des événements métier **au passé composé**, dans l'ordre chronologique. Le chemin nominal **et** les chemins d'échec.

| # | Événement | Déclenché quand... |
|---|-----------|--------------------|
| 1 | PanierValidé | le client valide son panier sur l'application |
| 2 | CommandeCréée | la commande est enregistrée dans le système |
| 3 | PaiementDemandé | la facture est transmise au service de paiement |
| 4 | PaiementAutorisé | le PSP (prestataire de paiement) valide la transaction |
| 5 | PaiementRefusé | le PSP refuse la transaction (échec) |
| 6 | CommandeAnnulée | le client annule ou le paiement échoue (échec) |
| 7 | PréparationLancée | le restaurant accepte de préparer la commande |
| 8 | CommandeLivrée | le livreur remet la commande au client |

## 2. Commandes (Passe 2)
| Commande | Acteur | Événement(s) résultant(s) |
|----------|--------|---------------------------|
| ValiderPanier | Client | PanierValidé |
| PasserCommande | Client | CommandeCréée |
| AutoriserPaiement | Système / PSP | PaiementAutorisé OU PaiementRefusé |
| AnnulerCommande | Client / Système | CommandeAnnulée |
| LancerPreparation | Restaurant | PréparationLancée |

## 3. Bounded Contexts retenus (Passes 3 & 4)
### Contexte 1 : Commande (`order-service` - Java/Spring Boot)
- **Agrégats** : Commande
- **Événements émis** : `PanierValidé`, `CommandeCréée`, `CommandeAnnulée`
- **Événements consommés** : `PaiementAutorisé`, `PaiementRefusé`

### Contexte 2 : Paiement (`payment-service` - TypeScript/NestJS, prévu TP3)
- **Agrégats** : Paiement
- **Événements émis** : `PaiementAutorisé`, `PaiementRefusé`

### Contexte 3 : Livraison & Restaurant (`delivery-service`)
- **Agrégats** : Livraison, Preparation
- **Événements émis** : `PréparationLancée`, `CommandeLivrée`

## 4. Contrats

### REST (Synchrone)
| Service | Endpoint | Usage |
|---------|----------|-------|
| `order-service` | `POST /orders` | Créer ou passer une nouvelle commande |
| `order-service` | `GET /orders/{id}` | Suivre l'état d'une commande |

### Événements (Asynchrone - Kafka / Futur)
| Événement | Émetteur | Consommateur(s) prévu(s) |
|-----------|----------|--------------------------|
| `order.created` | `order-service` | `payment-service`, `restaurant-service` |
| `payment.refused` | `payment-service` | `order-service` |

## Contrats REST (`order-service`)
| Méthode | Endpoint | Description / Usage | Code HTTP Succès |
|---------|----------|---------------------|------------------|
| `POST` | `/orders` | Créer une nouvelle commande | `201 Created` |
| `GET` | `/orders` | Lister toutes les commandes | `200 OK` |
| `GET` | `/orders/{id}` | Obtenir le détail d'une commande par son UUID | `200 OK` |
| `PATCH` | `/orders/{id}/status` | Changer le statut d'une commande (ex: PAID, CANCELLED) | `200 OK` |
| `DELETE` | `/orders/{id}` | Supprimer une commande | `204 No Content` |
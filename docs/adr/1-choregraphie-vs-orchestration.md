# ADR 001 — Choix de la chorégraphie pour la saga FoodFlow

## Statut

**Accepté** — cette décision est appliquée dans l'architecture événementielle développée durant les TP4 et TP5, notamment pour la saga de confirmation ou d'annulation d'une commande.

## Contexte

FoodFlow met en œuvre une **saga de compensation**. Lorsqu'une commande est créée, une demande d'autorisation de paiement est envoyée. Si le paiement est accepté, la commande est confirmée ; dans le cas contraire, elle est annulée afin de maintenir la cohérence du système.

Deux microservices interviennent dans ce processus :

- `order-service`, responsable de la gestion des commandes ;
- `payment-service`, responsable des décisions liées au paiement.

Les échanges entre ces services sont réalisés via **Kafka**.

Une décision d'architecture devait être prise concernant la coordination de cette saga :

- **chorégraphie**, où chaque service réagit de manière autonome aux événements ;
- **orchestration**, où un composant central pilote l'ensemble du processus.

Le choix est effectué en appliquant les critères étudiés en cours au contexte spécifique de FoodFlow.

---

## Analyse des critères

### Nombre d'étapes

La saga actuelle ne comporte que **deux étapes principales** :

1. `payment-service` décide si le paiement est accepté ou refusé ;
2. `order-service` confirme ou annule la commande selon cette décision.

Le flux reste donc relativement simple. Les bénéfices apportés par un orchestrateur sont davantage intéressants pour des processus plus longs impliquant de nombreuses transitions.

### Visibilité

Dans une approche chorégraphiée, il n'existe pas de composant possédant une vision complète de la saga.

Pour suivre son exécution, il est nécessaire d'examiner :

- les journaux des différents services ;
- les événements publiés sur `orders.events` et `payments.events`.

Avec seulement deux étapes et un identifiant commun (`orderId`), cette reconstruction reste relativement simple. Elle deviendrait plus complexe si la saga comportait davantage d'étapes.

### Répartition des responsabilités

Chaque microservice possède un domaine clairement défini :

- `order-service` gère le cycle de vie des commandes et décide de leur confirmation ou de leur annulation ;
- `payment-service` applique uniquement les règles de validation des paiements.

Cette séparation des responsabilités favorise naturellement une architecture basée sur la publication d'événements, sans qu'un composant central soit nécessaire.

### Évolution du processus

La logique métier actuelle évolue peu.

Le scénario « création de commande → paiement → confirmation ou annulation » est stable. Les avantages d'un orchestrateur, plus adaptés aux processus fréquemment modifiés, ne justifieraient donc pas le coût supplémentaire de sa mise en œuvre.

---

## Décision

Le choix retenu est celui de la **chorégraphie**.

Les microservices communiquent exclusivement par des événements Kafka, sans orchestrateur central.

Le fonctionnement est le suivant :

- `order-service` publie un événement `OrderCreated`, puis attend la réponse du service de paiement ;
- `payment-service` consomme cet événement, traite la demande et publie soit `PaymentAuthorized`, soit `PaymentRefused` ;
- `order-service` consomme ensuite cette réponse afin de confirmer ou d'annuler la commande. En cas d'échec, il publie également `OrderCancelled`, représentant l'étape de compensation.

Les échanges utilisent systématiquement `orderId` comme clé Kafka afin de garantir l'ordre des événements pour une même commande.

Le traitement des doublons est assuré grâce à une table `processed_events`, tandis que la fiabilité est renforcée par des mécanismes de **retries** et de **Dead Letter Queue (DLQ)**.

Cette décision est cohérente avec les caractéristiques actuelles de FoodFlow :

- peu d'étapes ;
- responsabilités bien séparées ;
- logique métier stable ;
- visibilité encore facilement exploitable.

---

## Solutions étudiées

### Option A — Chorégraphie (retenue)

Chaque microservice réagit aux événements qu'il reçoit puis publie, si nécessaire, de nouveaux événements.

**Avantages**

- faible couplage entre les services ;
- ajout de nouveaux consommateurs sans modifier les services existants ;
- absence de composant supplémentaire à développer ou maintenir ;
- aucun point central pouvant bloquer l'ensemble de la saga.

**Inconvénients**

- la progression de la saga est répartie entre plusieurs services ;
- la gestion des délais d'attente (timeouts) n'est pas prise en charge nativement.

---

### Option B — Orchestration

Une seconde possibilité consistait à introduire un orchestrateur chargé d'envoyer des commandes (`payment.commands`) et de piloter une machine à états (`EN_ATTENTE_PAIEMENT`, `CONFIRMEE`, `EN_COMPENSATION`, `ANNULEE`).

Cette architecture est décrite plus en détail dans le document `saga.md`.

**Avantages**

- prise en charge native des timeouts ;
- vision centralisée de l'état des sagas ;
- débogage plus simple grâce à une machine à états unique.

**Inconvénients**

- augmentation du couplage entre les services ;
- ajout d'un composant supplémentaire à développer et à maintenir ;
- présence d'un point central dont une panne peut interrompre toutes les sagas.

Pour une saga composée de seulement deux étapes, cette solution apparaît disproportionnée.

---

## Conséquences

### Points positifs

- Les microservices restent indépendants les uns des autres.
- De nouveaux services peuvent facilement s'abonner aux événements existants sans modifier les producteurs.
- L'architecture est simple et ne nécessite pas d'orchestrateur dédié.
- Aucun composant central ne constitue un point unique de défaillance.

### Limites acceptées

La chorégraphie présente néanmoins quelques contraintes.

Tout d'abord, **les timeouts ne sont pas gérés automatiquement**. Si `payment-service` cesse définitivement de répondre, la commande peut rester dans l'état `CREATED` sans qu'aucun mécanisme ne déclenche son annulation. Ce comportement nécessiterait l'ajout d'un processus externe chargé de détecter les commandes bloquées.

Par ailleurs, **l'état global de la saga n'est jamais stocké à un seul endroit**. Pour connaître la progression d'une commande, il faut consulter les journaux des services, le statut de la commande ainsi que les événements publiés sur Kafka. Cette contrainte reste acceptable avec une saga très courte.

Enfin, le problème de la **double écriture entre la base de données et Kafka** n'est pas traité dans ce projet. Une panne entre le commit en base et la publication de l'événement pourrait provoquer une incohérence. Cette problématique est généralement résolue à l'aide du **pattern Outbox**, qui reste hors du périmètre de ce projet.

---

## Réévaluation de cette décision

Cette décision pourra être revue si l'architecture évolue significativement.

Par exemple :

- ajout de nouvelles étapes (préparation, livraison, remboursement...) ;
- nécessité de gérer des délais d'attente métier ;
- besoin d'obtenir une vision centralisée de l'avancement des sagas.

Dans ce cas, l'approche orchestrée décrite dans `saga.md` deviendrait probablement plus adaptée.
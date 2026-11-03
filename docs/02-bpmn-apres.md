# Processus cible — réservation automatisée avec RoomOps

**Activité BC02 :** 2.3 (reconfiguration de processus), 2.6 (architecture applicative)
**Source :** `BookingService.createBooking`, contrainte `no_overlapping_booking` (Flyway `V1`)
**Nature :** diagramme de flux (et non BPMN 2.0)

```mermaid
flowchart LR
    A(["Employé connecté<br/>à l'application"]) --> B["Sélection de la salle,<br/>du créneau et des équipements"]
    B --> C["POST /api/v1/bookings"]
    C --> D{"Période valide<br/>et salle active ?"}
    D -->|"Non"| E["400 ou 409<br/>message explicite"]
    D -->|"Oui"| F{"Équipement demandé<br/>en panne ?"}
    F -->|"Oui"| E
    F -->|"Non"| G{"Chevauchement<br/>détecté ?"}
    G -->|"Oui"| E
    G -->|"Non"| H["INSERT atomique<br/>contrainte EXCLUDE GiST"]
    H --> I["201 Created<br/>réservation confirmée"]
    I --> J["Export iCalendar<br/>à la demande"]

    H -.->|"course concurrente :<br/>violation d'exclusion"| E

    style E fill:#fff3cd
    style H fill:#d4edda
    style I fill:#d4edda
```

## Lecture du diagramme

- La vérification applicative (`existsOverlapping`) donne un message métier explicite.
- La contrainte PostgreSQL `EXCLUDE USING GIST` reste le **garant ultime** : même si deux requêtes
  concurrentes passent la vérification applicative, une seule insertion aboutit.
- Un équipement en panne ne bloque que s'il est **réellement demandé** par la réservation
  (principe du choix éco-responsable).
- L'export iCalendar est une action distincte, déclenchée après la confirmation.

## Complément — déclaration de panne et annulation en cascade

Ce second flux est volontairement séparé : il ne fait pas partie de la création d'une réservation.

```mermaid
flowchart LR
    A(["Administrateur ou manager<br/>signale une panne"]) --> B["PUT /api/v1/equipments/{equipmentId}/status"]
    B --> C["EquipmentService.updateStatus<br/>statut = EN_PANNE"]
    C --> D["Recherche des réservations futures<br/>ayant sollicité cet équipement"]
    D --> E["Passage à ANNULEE<br/>raison_annulation = PANNE_EQUIPEMENT"]
    E --> F["Réponse : équipement +<br/>nombre de réservations annulées"]

    style C fill:#fff3cd
    style E fill:#d4edda
```

Cette séparation évite l'erreur fréquente qui consiste à représenter une annulation en cascade
au moment de la création d'une réservation. La cascade n'a lieu que lors du passage d'un
équipement à `EN_PANNE`.

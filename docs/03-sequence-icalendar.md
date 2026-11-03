# Séquence — export et import iCalendar

**Activités BC02 :** 2.6 (architecture applicative), 2.12 (intégrabilité)
**Source :** `BookingController`, `IcalService`, `api-contract/openapi.yaml`
**Endpoints réels :** `GET /api/v1/bookings/{id}/ical` et `POST /api/v1/bookings/import-ical`

```mermaid
sequenceDiagram
    autonumber
    actor U as Utilisateur
    participant F as Navigateur Angular
    participant B as Backend Spring Boot
    participant DB as PostgreSQL

    U->>F: Clique « Exporter » sur la réservation 42
    F->>B: GET /api/v1/bookings/42/ical
    B->>DB: SELECT booking + room
    DB-->>B: Données de la réservation
    B->>B: Construction du VCALENDAR avec ical4j
    B->>B: Ajout de X-ROOMOPS-ROOM-ID
    B-->>F: 200 text/calendar + fichier .ics
    F-->>U: Téléchargement du fichier

    Note over U,B: Le réimport passe par POST /api/v1/bookings/import-ical

    U->>F: Sélectionne le fichier .ics
    F->>B: POST /api/v1/bookings/import-ical<br/>Content-Type: text/calendar
    B->>B: Parsing ical4j
    B->>B: Extraction de X-ROOMOPS-ROOM-ID
    B->>DB: Vérification de la salle et de la période
    DB-->>B: Résultat

    alt Fichier invalide ou salle absente
        B-->>F: 400 Bad Request
    else Créneau en conflit ou salle indisponible
        B-->>F: 409 Conflict
    else Import valide
        B->>DB: INSERT booking
        B-->>F: 201 Created + BookingResponse
    end

    F-->>U: Réservation recréée
```

## Commentaire

- L'export produit un fichier **RFC 5545** lisible par Outlook, Google Agenda ou Apple Calendrier.
- La propriété `X-ROOMOPS-ROOM-ID` est indispensable : un fichier iCalendar générique ne référence
  aucune salle interne, donc il est refusé en `400`.
- L'import ne transporte pas les équipements sélectionnés : ils ne sont pas encodés dans l'export.
- L'import rejoue les contrôles métier de la création (période, salle active, équipement en panne,
  chevauchement), y compris la contrainte PostgreSQL.
- Le corps attendu est un binaire `text/calendar`, **pas** un formulaire `multipart`.

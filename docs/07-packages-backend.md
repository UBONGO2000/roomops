# Organisation des packages du backend

**Activités BC02 :** 2.6 (architecture en couches), 2.7 (degré de réutilisation)
**Source :** arborescence réelle de `backend/src/main/java` et interfaces générées depuis OpenAPI

```mermaid
flowchart TB
    subgraph Generated["api / model — générés depuis OpenAPI"]
        API["Interfaces API<br/>BookingsApi, RoomsApi, UsersApi..."]
        DTO["DTO<br/>BookingRequest, BookingResponse..."]
    end

    subgraph Controllers["controller"]
        BC[BookingController]
        CC[CompanyController]
        EC[EquipmentController]
        RC[RoomController]
        UC[UserController]
        AC[AuthController]
        GEH[GlobalExceptionHandler]
    end

    subgraph Services["service"]
        BS[BookingService]
        CS[CompanyService]
        ES[EquipmentService]
        RS[RoomService]
        US[UserService]
        AS[AuthService]
        IS[IcalService]
        PDE[PersonalDataExport]
        RA[RoomAvailability]
        TP[TokenPair]
    end

    subgraph Mappers["mapper"]
        BM[BookingMapper]
        CM[CompanyMapper]
        EM[EquipmentMapper]
        UM[UserMapper]
        IM[IcalMapper]
        DTM[DateTimeMapper]
        IBP[IcalBookingParams]
    end

    subgraph Repositories["repository"]
        BR[BookingRepository]
        BER[BookingEquipmentRepository]
        BuR[BuildingRepository]
        CR[CompanyRepository]
        ER[EquipmentRepository]
        RR[RoomRepository]
        UR[UserRepository]
    end

    subgraph Domain["domain — entités JPA et énumérations"]
        BE[Booking]
        BEE[BookingEquipment]
        UE[User]
        CE[Company]
        EE[Equipment]
        RE[Room]
        BU[Building]
        EN["BookingStatut<br/>EquipmentStatut<br/>RaisonAnnulation<br/>Role"]
    end

    subgraph Security["security"]
        JS[JwtService]
        JAF[JwtAuthenticationFilter]
        SC[SecurityConfig]
        CUP[CurrentUserProvider]
        TRS[TokenRevocationService]
        ITE[InvalidTokenException]
    end

    subgraph Exceptions["exception"]
        BCE[BookingConflictException]
        CHBE[CompanyHasBookingsException]
        EBE[EmployeeHasBookingsException]
        IBPE[InvalidBookingPeriodException]
        IESE[InvalidEquipmentSelectionException]
        IIFE[InvalidIcalFileException]
        OLCE[OptimisticLockConflictException]
    end

    Controllers --> Services
    Controllers --> Mappers
    Controllers --> Generated
    Services --> Repositories
    Services --> Mappers
    Services --> Security
    Mappers --> Domain
    Repositories --> Domain
    Services --> Exceptions
    Controllers --> Exceptions
    Exceptions --> GEH

    style Generated fill:#e3f2fd
    style Controllers fill:#e3f2fd
    style Services fill:#fff3cd
    style Repositories fill:#d4edda
    style Domain fill:#f8d7da
    style Security fill:#fce4ec
```

## Corrections apportées au diagramme initial

| Élément | Emplacement initial | Emplacement réel |
|---|---|---|
| `TokenRevocationService` | `service` | `security` |
| `GlobalExceptionHandler` | `exception` | `controller` |
| `RoomMapper` | `mapper` | n'existe pas (supprimé) |
| `Controllers → Mappers` | absent | les contrôleurs utilisent les mappers |

## Précisions sur les couches

- **`api` et `model` sont générés** par le plugin OpenAPI à partir de `api-contract/openapi.yaml`.
  Les contrôleurs implémentent les interfaces (`BookingController implements BookingsApi`) ;
  les DTO ne sont pas écrits à la main.
- **Les contrôleurs utilisent directement les mappers** pour convertir les entités en DTO.
  Ce n'est pas une couche de service pure, mais c'est le choix réel du projet.
- **`GlobalExceptionHandler`** est un `@RestControllerAdvice` placé dans `controller` : il traduit
  les exceptions métier en réponses HTTP (`400`, `401`, `403`, `404`, `409`).
- **`InvalidTokenException`** est dans `security`, pas dans `exception` : elle est interne à la
  chaîne d'authentification.
- **`TokenPair`**, `PersonalDataExport` et `RoomAvailability` sont des objets de service
  (records ou classes utilitaires), pas des entités.
- **Les énumérations** `BookingStatut`, `EquipmentStatut`, `RaisonAnnulation` et `Role` vivent
  dans `domain`, aux côtés des entités.

## Ce que le diagramme ne montre pas

- Les dépendances vers Spring Data JPA, Spring Security, ical4j et Redis.
- Le sens des dépendances vers les packages générés (`api`, `model`).
- Les tests, qui suivent la même arborescence dans `src/test/java`.

Ces éléments relèvent du texte de la section 2.6, pas nécessairement du diagramme.

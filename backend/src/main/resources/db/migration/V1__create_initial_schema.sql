-- Extension nécessaire pour la contrainte d'exclusion sur les créneaux (index GiST sur un type scalaire + un range)
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE company (
    id                   BIGSERIAL PRIMARY KEY,
    nom                  VARCHAR(255) NOT NULL,
    siret                VARCHAR(14),
    adresse_facturation  VARCHAR(500),
    tarif_horaire        NUMERIC(10, 2)
);

CREATE TABLE building (
    id      BIGSERIAL PRIMARY KEY,
    nom     VARCHAR(255) NOT NULL,
    adresse VARCHAR(500) NOT NULL
);

CREATE TABLE room (
    id          BIGSERIAL PRIMARY KEY,
    nom         VARCHAR(255) NOT NULL,
    capacite    INTEGER NOT NULL CHECK (capacite > 0),
    building_id BIGINT NOT NULL REFERENCES building (id),
    est_actif   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_room_building_id ON room (building_id);

CREATE TABLE equipment (
    id      BIGSERIAL PRIMARY KEY,
    type    VARCHAR(255) NOT NULL,
    room_id BIGINT NOT NULL REFERENCES room (id),
    statut  VARCHAR(20) NOT NULL DEFAULT 'OPERATIONNEL'
        CHECK (statut IN ('OPERATIONNEL', 'EN_PANNE'))
);

CREATE INDEX idx_equipment_room_id ON equipment (room_id);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nom           VARCHAR(255) NOT NULL,
    prenom        VARCHAR(255) NOT NULL,
    role          VARCHAR(20) NOT NULL
        CHECK (role IN ('SUPER_ADMIN', 'MANAGER', 'EMPLOYEE')),
    company_id    BIGINT REFERENCES company (id),
    -- Un Super-Admin n'appartient à aucune entreprise ; un Manager/Employé doit en avoir une.
    CONSTRAINT chk_user_company_by_role CHECK (
        (role = 'SUPER_ADMIN' AND company_id IS NULL)
        OR (role IN ('MANAGER', 'EMPLOYEE') AND company_id IS NOT NULL)
    )
);

CREATE INDEX idx_users_company_id ON users (company_id);

CREATE TABLE booking (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (id),
    room_id    BIGINT NOT NULL REFERENCES room (id),
    company_id BIGINT NOT NULL REFERENCES company (id),
    date_debut TIMESTAMP NOT NULL,
    date_fin   TIMESTAMP NOT NULL,
    motif      VARCHAR(500),
    statut     VARCHAR(20) NOT NULL DEFAULT 'CONFIRMEE'
        CHECK (statut IN ('CONFIRMEE', 'ANNULEE')),
    version    BIGINT NOT NULL DEFAULT 0,
    CHECK (date_fin > date_debut)
);

CREATE INDEX idx_booking_room_id ON booking (room_id);
CREATE INDEX idx_booking_user_id ON booking (user_id);
CREATE INDEX idx_booking_company_id ON booking (company_id);

-- Garantie d'intégrité ultime contre le double-booking : aucune insertion concurrente
-- ne peut faire chevaucher deux réservations actives sur la même salle, quel que soit
-- ce que l'application a vérifié (ou pas) avant d'insérer.
ALTER TABLE booking
    ADD CONSTRAINT no_overlapping_booking
    EXCLUDE USING GIST (
        room_id WITH =,
        tsrange(date_debut, date_fin) WITH &&
    ) WHERE (statut != 'ANNULEE');

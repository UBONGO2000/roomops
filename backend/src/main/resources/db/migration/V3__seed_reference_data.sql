-- Données de référence (bâtiments, salles, équipements) : le contrat API n'expose
-- volontairement aucun endpoint de création pour ces ressources (cf. mémoire — la
-- liste physique des salles change rarement, elle est provisionnée une fois par
-- migration plutôt que via un CRUD applicatif complet).
INSERT INTO building (nom, adresse) VALUES
    ('Bâtiment A', '12 rue de la Paix, 75002 Paris'),
    ('Bâtiment B', '8 avenue des Champs, 75008 Paris');

INSERT INTO room (nom, capacite, building_id, est_actif) VALUES
    ('Salle Alpha', 8, (SELECT id FROM building WHERE nom = 'Bâtiment A'), TRUE),
    ('Salle Beta', 4, (SELECT id FROM building WHERE nom = 'Bâtiment A'), TRUE),
    ('Salle Gamma', 12, (SELECT id FROM building WHERE nom = 'Bâtiment B'), TRUE);

INSERT INTO equipment (type, room_id, statut) VALUES
    ('Projecteur', (SELECT id FROM room WHERE nom = 'Salle Alpha'), 'OPERATIONNEL'),
    ('Visioconférence', (SELECT id FROM room WHERE nom = 'Salle Alpha'), 'OPERATIONNEL'),
    ('Tableau blanc', (SELECT id FROM room WHERE nom = 'Salle Beta'), 'OPERATIONNEL'),
    ('Projecteur', (SELECT id FROM room WHERE nom = 'Salle Gamma'), 'OPERATIONNEL'),
    ('Visioconférence', (SELECT id FROM room WHERE nom = 'Salle Gamma'), 'OPERATIONNEL');

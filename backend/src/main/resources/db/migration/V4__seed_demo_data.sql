-- Jeu de données de démonstration : deux sociétés locataires, un manager et deux employés par
-- société (nécessaire pour créer une réservation — BookingService.createBooking exige le rôle
-- EMPLOYEE ou MANAGER, que le seul compte existant avant cette migration, le Super-Admin de
-- V2, n'a jamais), et quinze salles supplémentaires pour porter le site à dix-huit salles au
-- total, conformément à la description du mémoire.
--
-- Identifiants de démo, UNIQUEMENT pour le développement local, la CI et la soutenance :
--   mot de passe (identique pour les six comptes) : Demo1234!
--
--   manager.nova@roomops.local    | MANAGER  | Nova Digital Studio
--   employe1.nova@roomops.local   | EMPLOYEE | Nova Digital Studio
--   employe2.nova@roomops.local   | EMPLOYEE | Nova Digital Studio
--   manager.kaizen@roomops.local  | MANAGER  | Atelier Kaizen Conseil
--   employe1.kaizen@roomops.local | EMPLOYEE | Atelier Kaizen Conseil
--   employe2.kaizen@roomops.local | EMPLOYEE | Atelier Kaizen Conseil
--
-- À ne jamais utiliser tel quel dans un environnement réellement exposé.

INSERT INTO company (nom, siret, adresse_facturation, tarif_horaire) VALUES
    ('Nova Digital Studio', '81234567800014', '45 rue du Faubourg Saint-Honoré, 75008 Paris', 45.00),
    ('Atelier Kaizen Conseil', '90256789100023', '22 boulevard Voltaire, 75011 Paris', 38.50);

-- Mot de passe en clair pour les six comptes : Demo1234! (empreinte BCrypt, coût 12, générée et
-- vérifiée avec le même org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder que
-- SecurityConfig, cf. corps de la PR).
INSERT INTO users (email, password_hash, nom, prenom, role, company_id) VALUES
    ('manager.nova@roomops.local', '$2a$12$eyvsDNNjpd7wehFJ/7ljUOGGIzkJ2G18KyiMUv2Hcajvrb3h2S2aW', 'Dubois', 'Claire', 'MANAGER',
        (SELECT id FROM company WHERE nom = 'Nova Digital Studio')),
    ('employe1.nova@roomops.local', '$2a$12$eyvsDNNjpd7wehFJ/7ljUOGGIzkJ2G18KyiMUv2Hcajvrb3h2S2aW', 'Haddad', 'Karim', 'EMPLOYEE',
        (SELECT id FROM company WHERE nom = 'Nova Digital Studio')),
    ('employe2.nova@roomops.local', '$2a$12$eyvsDNNjpd7wehFJ/7ljUOGGIzkJ2G18KyiMUv2Hcajvrb3h2S2aW', 'Lefèvre', 'Julie', 'EMPLOYEE',
        (SELECT id FROM company WHERE nom = 'Nova Digital Studio')),
    ('manager.kaizen@roomops.local', '$2a$12$eyvsDNNjpd7wehFJ/7ljUOGGIzkJ2G18KyiMUv2Hcajvrb3h2S2aW', 'Moreau', 'Thomas', 'MANAGER',
        (SELECT id FROM company WHERE nom = 'Atelier Kaizen Conseil')),
    ('employe1.kaizen@roomops.local', '$2a$12$eyvsDNNjpd7wehFJ/7ljUOGGIzkJ2G18KyiMUv2Hcajvrb3h2S2aW', 'Rossi', 'Sofia', 'EMPLOYEE',
        (SELECT id FROM company WHERE nom = 'Atelier Kaizen Conseil')),
    ('employe2.kaizen@roomops.local', '$2a$12$eyvsDNNjpd7wehFJ/7ljUOGGIzkJ2G18KyiMUv2Hcajvrb3h2S2aW', 'Petit', 'Nicolas', 'EMPLOYEE',
        (SELECT id FROM company WHERE nom = 'Atelier Kaizen Conseil'));

-- Quinze salles supplémentaires (suite de l'alphabet grec entamé par V3 : Alpha, Beta, Gamma),
-- réparties entre les deux bâtiments existants, pour porter le site à dix-huit salles au total.
INSERT INTO room (nom, capacite, building_id, est_actif) VALUES
    ('Salle Delta', 6, (SELECT id FROM building WHERE nom = 'Bâtiment A'), TRUE),
    ('Salle Epsilon', 10, (SELECT id FROM building WHERE nom = 'Bâtiment A'), TRUE),
    ('Salle Zeta', 2, (SELECT id FROM building WHERE nom = 'Bâtiment A'), TRUE),
    ('Salle Eta', 15, (SELECT id FROM building WHERE nom = 'Bâtiment B'), TRUE),
    ('Salle Theta', 4, (SELECT id FROM building WHERE nom = 'Bâtiment A'), TRUE),
    ('Salle Iota', 8, (SELECT id FROM building WHERE nom = 'Bâtiment B'), TRUE),
    ('Salle Kappa', 20, (SELECT id FROM building WHERE nom = 'Bâtiment B'), TRUE),
    ('Salle Lambda', 6, (SELECT id FROM building WHERE nom = 'Bâtiment A'), TRUE),
    ('Salle Mu', 3, (SELECT id FROM building WHERE nom = 'Bâtiment B'), TRUE),
    ('Salle Nu', 12, (SELECT id FROM building WHERE nom = 'Bâtiment A'), TRUE),
    ('Salle Xi', 5, (SELECT id FROM building WHERE nom = 'Bâtiment B'), TRUE),
    ('Salle Omicron', 9, (SELECT id FROM building WHERE nom = 'Bâtiment A'), TRUE),
    ('Salle Pi', 4, (SELECT id FROM building WHERE nom = 'Bâtiment B'), TRUE),
    ('Salle Rho', 18, (SELECT id FROM building WHERE nom = 'Bâtiment A'), TRUE),
    ('Salle Sigma', 7, (SELECT id FROM building WHERE nom = 'Bâtiment B'), TRUE);

-- Équipements sur une partie seulement des nouvelles salles : les autres restent volontairement
-- sans équipement, pour démontrer qu'une salle sans équipement reste réservable, à la différence
-- d'une salle dont un équipement est en panne.
INSERT INTO equipment (type, room_id, statut) VALUES
    ('Projecteur', (SELECT id FROM room WHERE nom = 'Salle Delta'), 'OPERATIONNEL'),
    ('Visioconférence', (SELECT id FROM room WHERE nom = 'Salle Eta'), 'OPERATIONNEL'),
    ('Projecteur', (SELECT id FROM room WHERE nom = 'Salle Eta'), 'OPERATIONNEL'),
    ('Visioconférence', (SELECT id FROM room WHERE nom = 'Salle Kappa'), 'OPERATIONNEL'),
    ('Projecteur', (SELECT id FROM room WHERE nom = 'Salle Kappa'), 'OPERATIONNEL'),
    ('Tableau blanc', (SELECT id FROM room WHERE nom = 'Salle Kappa'), 'OPERATIONNEL'),
    ('Tableau blanc', (SELECT id FROM room WHERE nom = 'Salle Lambda'), 'OPERATIONNEL'),
    ('Projecteur', (SELECT id FROM room WHERE nom = 'Salle Nu'), 'OPERATIONNEL'),
    ('Visioconférence', (SELECT id FROM room WHERE nom = 'Salle Rho'), 'OPERATIONNEL'),
    ('Tableau blanc', (SELECT id FROM room WHERE nom = 'Salle Rho'), 'OPERATIONNEL');

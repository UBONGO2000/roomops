-- Compte Super-Admin de démarrage : nécessaire car aucun autre mécanisme ne permet
-- de créer le premier utilisateur (les endpoints de création d'employés sont
-- réservés au Manager/Super-Admin, et un Super-Admin ne peut pas se créer lui-même).
--
-- Identifiants de démo, UNIQUEMENT pour le développement local et la CI :
--   email    : admin@roomops.local
--   password : ChangeMe123!
-- À ne jamais utiliser tel quel dans un environnement réellement exposé.
INSERT INTO users (email, password_hash, nom, prenom, role, company_id)
VALUES (
    'admin@roomops.local',
    '$2a$12$p6wmYpgC.FddWUMNc2IoH.BjYv2SsX7iKvEfX21iJqglXrYyuwYVq',
    'Admin',
    'RoomOps',
    'SUPER_ADMIN',
    NULL
);

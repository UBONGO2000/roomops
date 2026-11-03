# Séquence — anonymisation RGPD et révocation des jetons

**Activités BC02 :** 2.2 (conformité à la gouvernance), 2.12 (mise en exploitation)
**Source :** `UserController.anonymizeUser`, `UserService.anonymizeCurrentUser`,
`TokenRevocationService`, `JwtAuthenticationFilter`, `profile.ts`

```mermaid
sequenceDiagram
    autonumber
    actor U as Utilisateur
    participant F as Navigateur Angular
    participant B as Backend Spring Boot
    participant DB as PostgreSQL
    participant R as Redis

    U->>F: Confirme la suppression de son compte
    F->>B: DELETE /api/v1/users/anonymize
    B->>B: Capture l'email original avant mutation
    B->>DB: UPDATE users SET email = deleted-user-ID@roomops.local
    B->>DB: UPDATE users SET nom = 'Utilisateur', prenom = 'Supprimé'
    B->>DB: UPDATE users SET password_hash = UUID aléatoire encodé
    B->>R: SET revoked-tokens:email-original = horodatage<br/>TTL 7 jours
    R-->>B: Clé enregistrée
    B-->>F: 204 No Content
    F->>F: logout() puis redirection vers /connexion
    F-->>U: Session terminée

    Note over U,R: Tentative ultérieure avec un jeton émis avant l'anonymisation

    U->>B: Requête avec un ancien jeton
    B->>R: GET revoked-tokens:email-du-jeton
    R-->>B: Horodatage de révocation
    B->>B: iat du jeton <= horodatage ?
    B-->>B: Le filtre n'authentifie pas la requête
    B-->>U: 401 Unauthorized
```

## Commentaire

- Le verbe est **`DELETE`**, pas `POST` ; la réponse est **`204 No Content`**, pas `200 OK`.
- Les données personnelles remplacées sont l'email, le nom et le prénom ; le mot de passe devient
  un UUID aléatoire encodé, jamais communiqué.
- Les réservations sont conservées pour l'historique d'occupation des salles, mais elles ne sont
  plus rattachées à une identité exploitable.
- La révocation est volontairement **grossière** : une entrée par email, valant « tout jeton émis
  avant cet instant est invalide ». Le TTL de 7 jours correspond à la durée de vie maximale d'un
  refresh token.
- La déconnexion est orchestrée par le frontend après le `204` : le backend révoque, le navigateur
  ferme la session et redirige.
- Le filtre JWT ne renvoie pas lui-même le `401` : il laisse la requête non authentifiée, puis
  la chaîne de sécurité la rejette.

// Le déploiement de référence de ce projet est le docker-compose fourni : backend et frontend
// tournent sur la même machine, avec le port 8080 du conteneur backend publié sur l'hôte.
// Un vrai déploiement (domaines distincts) demanderait de rendre cette URL configurable au
// moment du build (ex: injection via un fichier config.json chargé au démarrage), hors
// périmètre ici.
export const environment = {
  production: true,
  apiBaseUrl: 'http://localhost:8080/api/v1',
};

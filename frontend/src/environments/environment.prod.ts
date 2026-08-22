// URL relative : le nginx de docker-compose.prod.yml sert le frontend ET relaie /api/ vers le
// service backend (cf. nginx.conf), donc le navigateur appelle toujours l'API en same-origin,
// quel que soit le domaine ou l'IP réellement utilisé pour accéder au site. Le port du backend
// n'est d'ailleurs plus publié sur l'hôte : il n'est joignable que depuis le réseau interne du
// compose, via ce proxy.
export const environment = {
  production: true,
  apiBaseUrl: '/api/v1',
};

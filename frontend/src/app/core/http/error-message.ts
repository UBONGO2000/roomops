import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/error.models';

// Une valeur de correspondance peut être un texte fixe (le statut a un sens indépendant du
// contenu du corps, ex: 403) ou une fonction lisant l'ApiError renvoyée par le serveur (le
// message backend varie selon le cas, ex: BOOKING_CONFLICT a plusieurs libellés possibles selon
// la règle métier violée, cf. BookingService).
export type ErrorMessageOverride = string | ((apiError: ApiError | undefined) => string);
export type ErrorMessageOverrides = Record<number, ErrorMessageOverride>;

const SERVER_UNREACHABLE = "Impossible de contacter le serveur. Vérifiez qu'il est démarré.";
const UNEXPECTED_ERROR = 'Une erreur inattendue est survenue. Merci de réessayer.';

/**
 * Traduit une erreur HTTP en message lisible pour l'utilisateur.
 *
 * Tronc commun : statut 0 (serveur injoignable, pas de corps à lire) et repli sur les champs
 * `code`/`message` du GlobalExceptionHandler pour tout le reste — ce dernier est ce qui permet
 * d'afficher le code d'erreur sans que chaque écran ait à le recopier.
 *
 * `overrides` laisse un composant surcharger un statut précis quand son sens dépend du contexte
 * (un 401 signifie « identifiants invalides » sur l'écran de connexion, mais « session expirée »
 * ailleurs), sans dupliquer cette logique de repli dans chaque composant.
 */
export function describeApiError(error: unknown, overrides: ErrorMessageOverrides = {}): string {
  if (!(error instanceof HttpErrorResponse)) {
    return UNEXPECTED_ERROR;
  }

  const apiError = error.error as ApiError | undefined;

  const override = overrides[error.status];
  if (override !== undefined) {
    return typeof override === 'function' ? override(apiError) : override;
  }

  if (error.status === 0) {
    return SERVER_UNREACHABLE;
  }

  if (apiError?.code && apiError.message) {
    return `${apiError.code} — ${apiError.message}`;
  }
  if (apiError?.message) {
    return apiError.message;
  }
  return UNEXPECTED_ERROR;
}

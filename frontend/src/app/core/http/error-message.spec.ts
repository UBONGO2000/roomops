import { HttpErrorResponse } from '@angular/common/http';
import { describeApiError } from './error-message';

function httpError(status: number, error?: unknown): HttpErrorResponse {
  return new HttpErrorResponse({ status, error });
}

describe('describeApiError', () => {
  it('returns a generic message when the error is not an HttpErrorResponse', () => {
    expect(describeApiError(new Error('boom'))).toBe(
      'Une erreur inattendue est survenue. Merci de réessayer.',
    );
  });

  it('reports the server as unreachable on status 0', () => {
    expect(describeApiError(httpError(0))).toBe(
      "Impossible de contacter le serveur. Vérifiez qu'il est démarré.",
    );
  });

  it('falls back to the code and message from the GlobalExceptionHandler body', () => {
    const error = httpError(403, { code: 'FORBIDDEN', message: 'Accès refusé' });
    expect(describeApiError(error)).toBe('FORBIDDEN — Accès refusé');
  });

  it('falls back to the message alone when no code is present', () => {
    const error = httpError(500, { message: 'Erreur inconnue' });
    expect(describeApiError(error)).toBe('Erreur inconnue');
  });

  it('falls back to the generic message when the body has neither code nor message', () => {
    expect(describeApiError(httpError(500))).toBe(
      'Une erreur inattendue est survenue. Merci de réessayer.',
    );
  });

  it('lets a string override replace the default message regardless of the body', () => {
    const error = httpError(403, { code: 'FORBIDDEN', message: 'Accès refusé' });
    expect(describeApiError(error, { 403: 'Session expirée, reconnectez-vous.' })).toBe(
      'Session expirée, reconnectez-vous.',
    );
  });

  it('lets a function override read the ApiError body to build a contextual message', () => {
    const error = httpError(409, {
      code: 'BOOKING_CONFLICT',
      message: "Cette salle n'est pas active",
    });
    const overrides = {
      409: (apiError: { message?: string } | undefined) =>
        apiError?.message ?? 'Conflit par défaut.',
    };
    expect(describeApiError(error, overrides)).toBe("Cette salle n'est pas active");
  });

  it('lets a function override fall back to its own default when the body has no message', () => {
    const overrides = {
      409: (apiError: { message?: string } | undefined) =>
        apiError?.message ?? 'Conflit par défaut.',
    };
    expect(describeApiError(httpError(409), overrides)).toBe('Conflit par défaut.');
  });

  it('applies an override for status 0 instead of the default unreachable message', () => {
    expect(describeApiError(httpError(0), { 0: 'Hors ligne.' })).toBe('Hors ligne.');
  });
});

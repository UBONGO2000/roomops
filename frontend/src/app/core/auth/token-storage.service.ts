import { Injectable } from '@angular/core';

const ACCESS_TOKEN_KEY = 'roomops.accessToken';
const REFRESH_TOKEN_KEY = 'roomops.refreshToken';

// localStorage plutôt qu'un cookie HttpOnly : le contrat /auth/login renvoie les deux tokens
// dans le corps JSON, pas via Set-Cookie (limite déjà documentée côté backend — exposition
// accrue au XSS par rapport à un cookie HttpOnly, assumée pour ce MVP).
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }

  setAccessToken(accessToken: string): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  }

  clear(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, RefreshTokenResponse, Role, TokenResponse } from '../models/auth.models';
import { decodeJwtPayload } from './jwt.util';
import { TokenStorageService } from './token-storage.service';

export interface CurrentUser {
  email: string;
  role: Role;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly currentUserSignal;

  readonly currentUser;
  readonly isAuthenticated;

  constructor(
    private readonly http: HttpClient,
    private readonly tokenStorage: TokenStorageService,
  ) {
    // Initialisés ici, pas en tant que champs de classe : un initialiseur de champ s'exécute
    // avant que le constructeur n'assigne les propriétés-paramètres (tokenStorage), et
    // readUserFromStoredToken() en a besoin immédiatement.
    this.currentUserSignal = signal<CurrentUser | null>(this.readUserFromStoredToken());
    this.currentUser = this.currentUserSignal.asReadonly();
    this.isAuthenticated = computed(() => this.currentUserSignal() !== null);
  }

  login(request: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${environment.apiBaseUrl}/auth/login`, request).pipe(
      tap((response) => {
        this.tokenStorage.setTokens(response.accessToken, response.refreshToken);
        this.currentUserSignal.set(this.readUserFromStoredToken());
      }),
    );
  }

  refreshAccessToken(): Observable<RefreshTokenResponse> {
    const refreshToken = this.tokenStorage.getRefreshToken();
    return this.http
      .post<RefreshTokenResponse>(`${environment.apiBaseUrl}/auth/refresh`, { refreshToken })
      .pipe(
        tap((response) => {
          this.tokenStorage.setAccessToken(response.accessToken);
          this.currentUserSignal.set(this.readUserFromStoredToken());
        }),
      );
  }

  logout(): void {
    this.tokenStorage.clear();
    this.currentUserSignal.set(null);
  }

  getAccessToken(): string | null {
    return this.tokenStorage.getAccessToken();
  }

  getRefreshToken(): string | null {
    return this.tokenStorage.getRefreshToken();
  }

  private readUserFromStoredToken(): CurrentUser | null {
    const token = this.tokenStorage.getAccessToken();
    if (!token) {
      return null;
    }
    const payload = decodeJwtPayload(token);
    if (!payload) {
      return null;
    }
    return { email: payload.sub, role: payload.role as Role };
  }
}

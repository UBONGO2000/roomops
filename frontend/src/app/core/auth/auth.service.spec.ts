import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { TokenResponse } from '../models/auth.models';
import { AuthService } from './auth.service';

// Un vrai JWT signé n'est pas nécessaire pour ces tests : seul le payload (2e segment,
// base64url) est lu par decodeJwtPayload, la signature n'est jamais vérifiée côté client.
function fakeAccessToken(email: string, role: string): string {
  const payload = { sub: email, role, type: 'access', iat: 0, exp: 9999999999 };
  const base64 = btoa(JSON.stringify(payload)).replace(/\+/g, '-').replace(/\//g, '_');
  return `header.${base64}.signature`;
}

describe('AuthService', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('starts unauthenticated when no token is stored', () => {
    const service = TestBed.inject(AuthService);

    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentUser()).toBeNull();
  });

  it('starts authenticated when a valid access token is already stored (page reload)', () => {
    localStorage.setItem('roomops.accessToken', fakeAccessToken('a@b.com', 'MANAGER'));

    const service = TestBed.inject(AuthService);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.currentUser()).toEqual({ email: 'a@b.com', role: 'MANAGER' });
  });

  it('login stores tokens and exposes the decoded current user', () => {
    const service = TestBed.inject(AuthService);
    const response: TokenResponse = {
      accessToken: fakeAccessToken('jean.dupont@techcorp.com', 'EMPLOYEE'),
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
    };

    service.login({ email: 'jean.dupont@techcorp.com', password: 'secret' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush(response);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.currentUser()).toEqual({ email: 'jean.dupont@techcorp.com', role: 'EMPLOYEE' });
    expect(service.getAccessToken()).toBe(response.accessToken);
    expect(service.getRefreshToken()).toBe('refresh-token');
  });

  it('logout clears tokens and the current user', () => {
    localStorage.setItem('roomops.accessToken', fakeAccessToken('a@b.com', 'EMPLOYEE'));
    localStorage.setItem('roomops.refreshToken', 'refresh-token');
    const service = TestBed.inject(AuthService);
    expect(service.isAuthenticated()).toBe(true);

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.getAccessToken()).toBeNull();
    expect(service.getRefreshToken()).toBeNull();
  });
});

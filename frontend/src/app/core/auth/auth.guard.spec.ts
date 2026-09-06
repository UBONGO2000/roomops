import {
  ActivatedRouteSnapshot,
  provideRouter,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { authGuard, roleGuard } from './auth.guard';
import { AuthService, CurrentUser } from './auth.service';

describe('authGuard', () => {
  let isAuthenticated: boolean;

  beforeEach(() => {
    isAuthenticated = false;
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthenticated: () => isAuthenticated } },
      ],
    });
  });

  it('allows navigation when the user is authenticated', () => {
    isAuthenticated = true;

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/reservations' } as RouterStateSnapshot),
    );

    expect(result).toBe(true);
  });

  it('redirects to /connexion with the attempted URL as a redirect query param when not authenticated', () => {
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/reservations' } as RouterStateSnapshot),
    ) as UrlTree;

    expect(result).toBeInstanceOf(UrlTree);
    expect(result.toString()).toBe('/connexion?redirect=%2Freservations');
  });
});

describe('roleGuard', () => {
  let currentUser: CurrentUser | null;

  beforeEach(() => {
    currentUser = null;
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { currentUser: () => currentUser } },
      ],
    });
  });

  it('allows navigation when the current user role is in the allowed list', () => {
    currentUser = { email: 'manager@techcorp.com', role: 'MANAGER' };
    const guard = roleGuard(['MANAGER', 'SUPER_ADMIN']);

    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

    expect(result).toBe(true);
  });

  it('redirects to / when the current user role is not in the allowed list', () => {
    currentUser = { email: 'employee@techcorp.com', role: 'EMPLOYEE' };
    const guard = roleGuard(['MANAGER']);

    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as UrlTree;

    expect(result.toString()).toBe('/');
  });

  it('redirects to / when there is no current user', () => {
    const guard = roleGuard(['MANAGER']);

    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as UrlTree;

    expect(result.toString()).toBe('/');
  });
});

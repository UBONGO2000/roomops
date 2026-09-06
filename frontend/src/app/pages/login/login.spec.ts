import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';
import { Login } from './login';

describe('Login', () => {
  let authServiceStub: {
    isAuthenticated: ReturnType<typeof vi.fn>;
    login: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  function configureWithRedirect(redirect?: string): void {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceStub },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap(redirect ? { redirect } : {}) },
          },
        },
      ],
    });

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  }

  beforeEach(() => {
    authServiceStub = {
      isAuthenticated: vi.fn(() => false),
      login: vi.fn(),
    };
    configureWithRedirect();
  });

  it('redirects to /reservations immediately when already authenticated', () => {
    authServiceStub.isAuthenticated.mockReturnValue(true);

    TestBed.createComponent(Login);

    expect(router.navigateByUrl).toHaveBeenCalledWith('/reservations');
  });

  it('does not call login when the form is invalid', () => {
    const fixture = TestBed.createComponent(Login);
    const login = fixture.componentInstance;

    (login as unknown as { onSubmit(): void }).onSubmit();

    expect(authServiceStub.login).not.toHaveBeenCalled();
  });

  it('navigates to /reservations after a successful login with no redirect param', () => {
    authServiceStub.login.mockReturnValue(
      of({ accessToken: 'a', refreshToken: 'r', tokenType: 'Bearer' }),
    );
    const fixture = TestBed.createComponent(Login);
    const login = fixture.componentInstance as unknown as {
      form: { setValue(value: unknown): void };
      onSubmit(): void;
    };

    login.form.setValue({ email: 'jean.dupont@techcorp.com', password: 'ChangeMe123!' });
    login.onSubmit();

    expect(authServiceStub.login).toHaveBeenCalledWith({
      email: 'jean.dupont@techcorp.com',
      password: 'ChangeMe123!',
    });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/reservations');
  });

  it('navigates to the redirect query param after a successful login when present', () => {
    configureWithRedirect('/employes');
    authServiceStub.login.mockReturnValue(
      of({ accessToken: 'a', refreshToken: 'r', tokenType: 'Bearer' }),
    );
    const fixture = TestBed.createComponent(Login);
    const login = fixture.componentInstance as unknown as {
      form: { setValue(value: unknown): void };
      onSubmit(): void;
    };

    login.form.setValue({ email: 'manager@techcorp.com', password: 'ChangeMe123!' });
    login.onSubmit();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/employes');
  });

  it('sets a dedicated error message on a 401 response', () => {
    authServiceStub.login.mockReturnValue(
      throwError(
        () => new HttpErrorResponse({ status: 401, error: { code: 'INVALID_CREDENTIALS' } }),
      ),
    );
    const fixture = TestBed.createComponent(Login);
    const login = fixture.componentInstance as unknown as {
      form: { setValue(value: unknown): void };
      onSubmit(): void;
    };

    login.form.setValue({ email: 'jean.dupont@techcorp.com', password: 'wrong' });
    login.onSubmit();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Email ou mot de passe incorrect.');
  });

  it('toggles password field visibility', () => {
    const fixture = TestBed.createComponent(Login);
    const login = fixture.componentInstance as unknown as {
      passwordVisible: () => boolean;
      togglePasswordVisibility(): void;
    };

    expect(login.passwordVisible()).toBe(false);
    login.togglePasswordVisibility();
    expect(login.passwordVisible()).toBe(true);
  });
});

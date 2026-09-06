import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceStub: {
    getAccessToken: ReturnType<typeof vi.fn>;
    getRefreshToken: ReturnType<typeof vi.fn>;
    refreshAccessToken: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  beforeEach(() => {
    authServiceStub = {
      getAccessToken: vi.fn(() => 'access-token'),
      getRefreshToken: vi.fn(() => 'refresh-token'),
      refreshAccessToken: vi.fn(),
      logout: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceStub },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => httpMock.verify());

  it('adds the Authorization header to API requests when an access token is available', () => {
    http.get(`${environment.apiBaseUrl}/bookings`).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bookings`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer access-token');
    req.flush({});
  });

  it('does not add the Authorization header to the login endpoint', () => {
    http.post(`${environment.apiBaseUrl}/auth/login`, {}).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('does not add the Authorization header to requests outside the API base URL', () => {
    http.get('https://other-service.example.com/data').subscribe();

    const req = httpMock.expectOne('https://other-service.example.com/data');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('refreshes the token once on a 401 and retries the original request', () => {
    authServiceStub.refreshAccessToken.mockReturnValue(
      of({ accessToken: 'new-access-token', tokenType: 'Bearer' }),
    );

    let result: unknown;
    http.get(`${environment.apiBaseUrl}/bookings`).subscribe((response) => (result = response));

    const firstReq = httpMock.expectOne(`${environment.apiBaseUrl}/bookings`);
    firstReq.flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(authServiceStub.refreshAccessToken).toHaveBeenCalledTimes(1);

    const retriedReq = httpMock.expectOne(`${environment.apiBaseUrl}/bookings`);
    expect(retriedReq.request.headers.get('Authorization')).toBe('Bearer new-access-token');
    retriedReq.flush({ ok: true });

    expect(result).toEqual({ ok: true });
    expect(authServiceStub.logout).not.toHaveBeenCalled();
  });

  it('logs out and redirects to /connexion when the refresh itself fails', () => {
    authServiceStub.refreshAccessToken.mockReturnValue(
      throwError(() => new Error('refresh failed')),
    );

    let erroredOut = false;
    http.get(`${environment.apiBaseUrl}/bookings`).subscribe({
      error: () => (erroredOut = true),
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bookings`);
    req.flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(erroredOut).toBe(true);
    expect(authServiceStub.logout).toHaveBeenCalledTimes(1);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/connexion');
  });

  it('does not attempt a refresh when no refresh token is available', () => {
    authServiceStub.getRefreshToken.mockReturnValue(null);

    let erroredOut = false;
    http.get(`${environment.apiBaseUrl}/bookings`).subscribe({
      error: () => (erroredOut = true),
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bookings`);
    req.flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(erroredOut).toBe(true);
    expect(authServiceStub.refreshAccessToken).not.toHaveBeenCalled();
  });

  it('does not attempt a refresh on a 401 from the login endpoint itself', () => {
    let erroredOut = false;
    http.post(`${environment.apiBaseUrl}/auth/login`, {}).subscribe({
      error: () => (erroredOut = true),
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    req.flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(erroredOut).toBe(true);
    expect(authServiceStub.refreshAccessToken).not.toHaveBeenCalled();
  });
});

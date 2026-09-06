import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { UserDataExport, UserResponse } from '../models/auth.models';
import { UserService } from './user.service';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getCurrentUser performs a GET on /users/me', () => {
    const user: UserResponse = {
      id: 1,
      email: 'a@b.com',
      nom: 'Dupont',
      prenom: 'Jean',
      role: 'EMPLOYEE',
    };

    service.getCurrentUser().subscribe((result) => expect(result).toEqual(user));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/users/me`);
    expect(req.request.method).toBe('GET');
    req.flush(user);
  });

  it('exportUserData performs a GET on /users/export', () => {
    const data: UserDataExport = {
      user: { id: 1, email: 'a@b.com', nom: 'Dupont', prenom: 'Jean', role: 'EMPLOYEE' },
      bookings: [],
      exportDate: '2030-01-15T00:00:00.000Z',
    };

    service.exportUserData().subscribe((result) => expect(result).toEqual(data));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/users/export`);
    expect(req.request.method).toBe('GET');
    req.flush(data);
  });

  it('anonymizeUser performs a DELETE on /users/anonymize', () => {
    service.anonymizeUser().subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/users/anonymize`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { UserResponse } from '../models/auth.models';
import { CompanyResponse } from '../models/company.models';
import { CompanyService } from './company.service';

describe('CompanyService', () => {
  let service: CompanyService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CompanyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listCompanies performs a GET on /companies', () => {
    const companies: CompanyResponse[] = [{ id: 1, nom: 'TechCorp' }];

    service.listCompanies().subscribe((result) => expect(result).toEqual(companies));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/companies`);
    expect(req.request.method).toBe('GET');
    req.flush(companies);
  });

  it('createCompany performs a POST with the given payload', () => {
    const response: CompanyResponse = { id: 2, nom: 'Nova Digital Studio' };

    service
      .createCompany({ nom: 'Nova Digital Studio' })
      .subscribe((result) => expect(result).toEqual(response));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/companies`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nom: 'Nova Digital Studio' });
    req.flush(response);
  });

  it('deleteCompany performs a DELETE on /companies/{id}', () => {
    service.deleteCompany(2).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/companies/2`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('getCompanyEmployees performs a GET on /companies/{id}/employees', () => {
    const employees: UserResponse[] = [
      { id: 1, email: 'a@b.com', nom: 'Dupont', prenom: 'Jean', role: 'EMPLOYEE', companyId: 2 },
    ];

    service.getCompanyEmployees(2).subscribe((result) => expect(result).toEqual(employees));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/companies/2/employees`);
    expect(req.request.method).toBe('GET');
    req.flush(employees);
  });

  it('addEmployee performs a POST on /companies/{id}/employees with the given payload', () => {
    const response: UserResponse = {
      id: 3,
      email: 'nouvel.employe@b.com',
      nom: 'Martin',
      prenom: 'Alice',
      role: 'EMPLOYEE',
      companyId: 2,
    };

    service
      .addEmployee(2, {
        email: 'nouvel.employe@b.com',
        password: 'ChangeMe123!',
        nom: 'Martin',
        prenom: 'Alice',
      })
      .subscribe((result) => expect(result).toEqual(response));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/companies/2/employees`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.email).toBe('nouvel.employe@b.com');
    req.flush(response);
  });

  it('removeEmployee performs a DELETE on /companies/{companyId}/employees/{userId}', () => {
    service.removeEmployee(2, 3).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/companies/2/employees/3`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});

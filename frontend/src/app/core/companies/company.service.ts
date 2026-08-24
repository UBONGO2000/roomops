import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateEmployeeRequest, UserResponse } from '../models/auth.models';
import { CompanyRequest, CompanyResponse } from '../models/company.models';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  constructor(private readonly http: HttpClient) {}

  listCompanies(): Observable<CompanyResponse[]> {
    return this.http.get<CompanyResponse[]>(`${environment.apiBaseUrl}/companies`);
  }

  createCompany(request: CompanyRequest): Observable<CompanyResponse> {
    return this.http.post<CompanyResponse>(`${environment.apiBaseUrl}/companies`, request);
  }

  getCompanyEmployees(companyId: number): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(
      `${environment.apiBaseUrl}/companies/${companyId}/employees`,
    );
  }

  addEmployee(companyId: number, request: CreateEmployeeRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(
      `${environment.apiBaseUrl}/companies/${companyId}/employees`,
      request,
    );
  }

  removeEmployee(companyId: number, userId: number): Observable<void> {
    return this.http.delete<void>(
      `${environment.apiBaseUrl}/companies/${companyId}/employees/${userId}`,
    );
  }
}

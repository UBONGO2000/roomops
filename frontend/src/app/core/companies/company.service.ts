import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserResponse } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  constructor(private readonly http: HttpClient) {}

  getCompanyEmployees(companyId: number): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(
      `${environment.apiBaseUrl}/companies/${companyId}/employees`,
    );
  }
}

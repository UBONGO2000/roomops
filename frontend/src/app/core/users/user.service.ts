import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserDataExport, UserResponse } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(private readonly http: HttpClient) {}

  getCurrentUser(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${environment.apiBaseUrl}/users/me`);
  }

  exportUserData(): Observable<UserDataExport> {
    return this.http.get<UserDataExport>(`${environment.apiBaseUrl}/users/export`);
  }

  anonymizeUser(): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/users/anonymize`);
  }
}

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AvailabilityResponse, RoomResponse } from '../models/room.models';

@Injectable({ providedIn: 'root' })
export class RoomService {
  constructor(private readonly http: HttpClient) {}

  listRooms(): Observable<RoomResponse[]> {
    return this.http.get<RoomResponse[]>(`${environment.apiBaseUrl}/rooms`);
  }

  checkAvailability(
    roomId: number,
    dateDebut: string,
    dateFin: string,
  ): Observable<AvailabilityResponse> {
    const params = new HttpParams().set('dateDebut', dateDebut).set('dateFin', dateFin);
    return this.http.get<AvailabilityResponse>(
      `${environment.apiBaseUrl}/rooms/${roomId}/availability`,
      { params },
    );
  }
}

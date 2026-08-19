import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BookingPageResponse,
  BookingRequest,
  BookingResponse,
  ListBookingsParams,
} from '../models/booking.models';

@Injectable({ providedIn: 'root' })
export class BookingService {
  constructor(private readonly http: HttpClient) {}

  listBookings(params: ListBookingsParams): Observable<BookingPageResponse> {
    let httpParams = new HttpParams();
    if (params.roomId !== undefined) {
      httpParams = httpParams.set('roomId', params.roomId);
    }
    if (params.dateDebut) {
      httpParams = httpParams.set('dateDebut', params.dateDebut);
    }
    if (params.dateFin) {
      httpParams = httpParams.set('dateFin', params.dateFin);
    }
    if (params.statut) {
      httpParams = httpParams.set('statut', params.statut);
    }
    httpParams = httpParams.set('page', params.page ?? 0).set('size', params.size ?? 50);

    return this.http.get<BookingPageResponse>(`${environment.apiBaseUrl}/bookings`, {
      params: httpParams,
    });
  }

  createBooking(request: BookingRequest): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(`${environment.apiBaseUrl}/bookings`, request);
  }

  cancelBooking(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/bookings/${id}`);
  }
}

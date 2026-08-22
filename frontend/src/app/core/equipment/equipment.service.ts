import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  EquipmentResponse,
  EquipmentStatusUpdateResponse,
  EquipmentStatut,
} from '../models/equipment.models';

@Injectable({ providedIn: 'root' })
export class EquipmentService {
  constructor(private readonly http: HttpClient) {}

  listEquipments(): Observable<EquipmentResponse[]> {
    return this.http.get<EquipmentResponse[]>(`${environment.apiBaseUrl}/equipments`);
  }

  updateStatus(
    equipmentId: number,
    statut: EquipmentStatut,
  ): Observable<EquipmentStatusUpdateResponse> {
    return this.http.put<EquipmentStatusUpdateResponse>(
      `${environment.apiBaseUrl}/equipments/${equipmentId}/status`,
      { statut },
    );
  }
}

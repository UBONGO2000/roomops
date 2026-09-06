import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { EquipmentResponse, EquipmentStatusUpdateResponse } from '../models/equipment.models';
import { EquipmentService } from './equipment.service';

describe('EquipmentService', () => {
  let service: EquipmentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EquipmentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listEquipments performs a GET on /equipments', () => {
    const equipments: EquipmentResponse[] = [
      { id: 1, type: 'Projecteur', roomId: 1, roomName: 'Salle Alpha', statut: 'OPERATIONNEL' },
    ];

    service.listEquipments().subscribe((result) => expect(result).toEqual(equipments));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/equipments`);
    expect(req.request.method).toBe('GET');
    req.flush(equipments);
  });

  it('updateStatus performs a PUT on /equipments/{id}/status with the new status', () => {
    const response: EquipmentStatusUpdateResponse = {
      id: 1,
      type: 'Projecteur',
      roomId: 1,
      roomName: 'Salle Alpha',
      statut: 'EN_PANNE',
      reservationsAnnulees: 2,
    };

    service.updateStatus(1, 'EN_PANNE').subscribe((result) => expect(result).toEqual(response));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/equipments/1/status`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ statut: 'EN_PANNE' });
    req.flush(response);
  });
});

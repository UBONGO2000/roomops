import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { RoomResponse } from '../models/room.models';
import { RoomService } from './room.service';

describe('RoomService', () => {
  let service: RoomService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RoomService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listRooms performs a GET on /rooms', () => {
    const rooms: RoomResponse[] = [
      {
        id: 1,
        nom: 'Salle Alpha',
        capacite: 8,
        buildingId: 1,
        buildingName: 'Bâtiment A',
        estActif: true,
      },
    ];

    service.listRooms().subscribe((result) => expect(result).toEqual(rooms));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/rooms`);
    expect(req.request.method).toBe('GET');
    req.flush(rooms);
  });

  it('checkAvailability sends dateDebut/dateFin as query params without equipmentIds when none given', () => {
    service
      .checkAvailability(1, '2030-01-15T09:00:00.000Z', '2030-01-15T11:00:00.000Z')
      .subscribe();

    const req = httpMock.expectOne(
      (request) => request.url === `${environment.apiBaseUrl}/rooms/1/availability`,
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('dateDebut')).toBe('2030-01-15T09:00:00.000Z');
    expect(req.request.params.get('dateFin')).toBe('2030-01-15T11:00:00.000Z');
    expect(req.request.params.has('equipmentIds')).toBe(false);
    req.flush({
      roomId: 1,
      roomName: 'Salle Alpha',
      isAvailable: true,
      dateDebut: '2030-01-15T09:00:00.000Z',
      dateFin: '2030-01-15T11:00:00.000Z',
    });
  });

  it('checkAvailability appends one equipmentIds param per selected equipment', () => {
    service
      .checkAvailability(1, '2030-01-15T09:00:00.000Z', '2030-01-15T11:00:00.000Z', [10, 20])
      .subscribe();

    const req = httpMock.expectOne(
      (request) => request.url === `${environment.apiBaseUrl}/rooms/1/availability`,
    );
    expect(req.request.params.getAll('equipmentIds')).toEqual(['10', '20']);
    req.flush({
      roomId: 1,
      roomName: 'Salle Alpha',
      isAvailable: false,
      reason: 'Panne : Projecteur',
      dateDebut: '2030-01-15T09:00:00.000Z',
      dateFin: '2030-01-15T11:00:00.000Z',
    });
  });
});

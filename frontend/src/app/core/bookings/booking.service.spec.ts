import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { BookingPageResponse, BookingResponse } from '../models/booking.models';
import { BookingService } from './booking.service';

describe('BookingService', () => {
  let service: BookingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BookingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listBookings defaults page/size and omits filters that are not provided', () => {
    const page: BookingPageResponse = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 50,
    };

    service.listBookings({}).subscribe((result) => expect(result).toEqual(page));

    const req = httpMock.expectOne(
      (request) => request.url === `${environment.apiBaseUrl}/bookings`,
    );
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('50');
    expect(req.request.params.has('roomId')).toBe(false);
    expect(req.request.params.has('statut')).toBe(false);
    req.flush(page);
  });

  it('listBookings forwards every provided filter as a query param', () => {
    service
      .listBookings({
        roomId: 3,
        dateDebut: '2030-01-01T00:00:00.000Z',
        dateFin: '2030-01-08T00:00:00.000Z',
        statut: 'CONFIRMEE',
        page: 2,
        size: 10,
      })
      .subscribe();

    const req = httpMock.expectOne(
      (request) => request.url === `${environment.apiBaseUrl}/bookings`,
    );
    expect(req.request.params.get('roomId')).toBe('3');
    expect(req.request.params.get('dateDebut')).toBe('2030-01-01T00:00:00.000Z');
    expect(req.request.params.get('dateFin')).toBe('2030-01-08T00:00:00.000Z');
    expect(req.request.params.get('statut')).toBe('CONFIRMEE');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 2, size: 10 });
  });

  it('createBooking performs a POST with the given payload', () => {
    const response: BookingResponse = {
      id: 1,
      roomId: 1,
      roomName: 'Salle Alpha',
      userId: 5,
      userName: 'Jean Dupont',
      companyName: 'TechCorp',
      dateDebut: '2030-01-15T09:00:00.000Z',
      dateFin: '2030-01-15T11:00:00.000Z',
      statut: 'CONFIRMEE',
      version: 0,
    };

    service
      .createBooking({
        roomId: 1,
        dateDebut: '2030-01-15T09:00:00.000Z',
        dateFin: '2030-01-15T11:00:00.000Z',
        equipmentIds: [7],
      })
      .subscribe((result) => expect(result).toEqual(response));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bookings`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.equipmentIds).toEqual([7]);
    req.flush(response);
  });

  it('cancelBooking performs a DELETE on /bookings/{id}', () => {
    service.cancelBooking(42).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bookings/42`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('exportBookingIcal performs a GET returning a blob', () => {
    service.exportBookingIcal(7).subscribe((result) => expect(result).toBeInstanceOf(Blob));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bookings/7/ical`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['BEGIN:VCALENDAR'], { type: 'text/calendar' }));
  });
});

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { AuthService, CurrentUser } from '../../core/auth/auth.service';
import { BookingService } from '../../core/bookings/booking.service';
import { startOfWeek, toDateInputValue } from '../../core/date.util';
import { BookingPageResponse, BookingResponse } from '../../core/models/booking.models';
import { BookingList } from './booking-list';

describe('BookingList', () => {
  let bookingServiceStub: {
    listBookings: ReturnType<typeof vi.fn>;
    cancelBooking: ReturnType<typeof vi.fn>;
    exportBookingIcal: ReturnType<typeof vi.fn>;
  };
  let currentUserSignal: ReturnType<typeof signal<CurrentUser | null>>;

  function pageOf(content: BookingResponse[]): BookingPageResponse {
    return { content, totalElements: content.length, totalPages: 1, number: 0, size: 100 };
  }

  function bookingAt(id: number, isoDate: string): BookingResponse {
    return {
      id,
      roomId: 1,
      roomName: 'Salle Alpha',
      userId: 5,
      userName: 'Jean Dupont',
      companyName: 'TechCorp',
      dateDebut: `${isoDate}T09:00:00.000Z`,
      dateFin: `${isoDate}T10:00:00.000Z`,
      statut: 'CONFIRMEE',
      version: 0,
    };
  }

  beforeEach(() => {
    currentUserSignal = signal<CurrentUser | null>({
      email: 'jean.dupont@techcorp.com',
      role: 'EMPLOYEE',
    });
    bookingServiceStub = {
      listBookings: vi.fn(() => of(pageOf([]))),
      cancelBooking: vi.fn(),
      exportBookingIcal: vi.fn(),
    };

    TestBed.configureTestingModule({
      imports: [BookingList],
      providers: [
        provideRouter([]),
        { provide: BookingService, useValue: bookingServiceStub },
        { provide: AuthService, useValue: { currentUser: currentUserSignal } },
      ],
    });
  });

  it('loads the current week of bookings on construction', () => {
    TestBed.createComponent(BookingList);
    expect(bookingServiceStub.listBookings).toHaveBeenCalledTimes(1);
  });

  it('shows an error message when loading bookings fails', () => {
    bookingServiceStub.listBookings.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );
    const fixture = TestBed.createComponent(BookingList);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Impossible de charger vos réservations');
  });

  it('groups bookings returned by the API under their day', () => {
    // Même format que BookingList.toIsoDate (parties de date locales), pour que la réservation
    // tombe bien dans l'un des groupes de weekDays() calculés à partir d'aujourd'hui.
    const mondayIso = toDateInputValue(startOfWeek(new Date()));
    bookingServiceStub.listBookings.mockReturnValue(of(pageOf([bookingAt(1, mondayIso)])));

    const fixture = TestBed.createComponent(BookingList);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Salle Alpha');
  });

  it('reloads the previous week when navigating backwards', () => {
    const fixture = TestBed.createComponent(BookingList);
    fixture.detectChanges();
    bookingServiceStub.listBookings.mockClear();

    (fixture.componentInstance as unknown as { previousWeek(): void }).previousWeek();

    expect(bookingServiceStub.listBookings).toHaveBeenCalledTimes(1);
  });

  it('reloads the next week when navigating forwards', () => {
    const fixture = TestBed.createComponent(BookingList);
    fixture.detectChanges();
    bookingServiceStub.listBookings.mockClear();

    (fixture.componentInstance as unknown as { nextWeek(): void }).nextWeek();

    expect(bookingServiceStub.listBookings).toHaveBeenCalledTimes(1);
  });

  it('does nothing when the cancellation confirmation is declined', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    const fixture = TestBed.createComponent(BookingList);
    fixture.detectChanges();

    (
      fixture.componentInstance as unknown as { cancelBooking(booking: BookingResponse): void }
    ).cancelBooking(bookingAt(1, '2030-01-15'));

    expect(bookingServiceStub.cancelBooking).not.toHaveBeenCalled();
  });

  it('cancels the booking and reloads the week when confirmed', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    bookingServiceStub.cancelBooking.mockReturnValue(of(undefined));
    const fixture = TestBed.createComponent(BookingList);
    fixture.detectChanges();
    bookingServiceStub.listBookings.mockClear();

    (
      fixture.componentInstance as unknown as { cancelBooking(booking: BookingResponse): void }
    ).cancelBooking(bookingAt(1, '2030-01-15'));

    expect(bookingServiceStub.cancelBooking).toHaveBeenCalledWith(1);
    expect(bookingServiceStub.listBookings).toHaveBeenCalledTimes(1);
  });

  it('shows an error message when cancellation fails', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    bookingServiceStub.cancelBooking.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );
    const fixture = TestBed.createComponent(BookingList);
    fixture.detectChanges();

    (
      fixture.componentInstance as unknown as { cancelBooking(booking: BookingResponse): void }
    ).cancelBooking(bookingAt(1, '2030-01-15'));
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain("Impossible d'annuler cette réservation");
  });

  it('downloads an iCalendar file when export succeeds', () => {
    bookingServiceStub.exportBookingIcal.mockReturnValue(
      of(new Blob(['BEGIN:VCALENDAR'], { type: 'text/calendar' })),
    );
    const createObjectURL = vi.fn(() => 'blob:ical-url');
    const revokeObjectURL = vi.fn();
    vi.spyOn(URL, 'createObjectURL').mockImplementation(createObjectURL);
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(revokeObjectURL);
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
    const fixture = TestBed.createComponent(BookingList);
    fixture.detectChanges();

    (fixture.componentInstance as unknown as { exportIcal(booking: BookingResponse): void }).exportIcal(
      bookingAt(7, '2030-01-15'),
    );

    expect(bookingServiceStub.exportBookingIcal).toHaveBeenCalledWith(7);
    expect(createObjectURL).toHaveBeenCalled();
    expect(clickSpy).toHaveBeenCalled();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:ical-url');
    vi.restoreAllMocks();
  });
});

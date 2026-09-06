import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { BookingService } from '../../core/bookings/booking.service';
import { BookingResponse } from '../../core/models/booking.models';
import { AvailabilityResponse, RoomResponse } from '../../core/models/room.models';
import { RoomService } from '../../core/rooms/room.service';
import { BookingForm } from './booking-form';

describe('BookingForm', () => {
  let roomServiceStub: {
    listRooms: ReturnType<typeof vi.fn>;
    checkAvailability: ReturnType<typeof vi.fn>;
  };
  let bookingServiceStub: { createBooking: ReturnType<typeof vi.fn> };
  let snackBarStub: { open: ReturnType<typeof vi.fn> };
  let router: Router;

  const roomWithEquipment: RoomResponse = {
    id: 1,
    nom: 'Salle Alpha',
    capacite: 8,
    buildingId: 1,
    buildingName: 'Bâtiment A',
    estActif: true,
    equipements: [
      { id: 10, type: 'Projecteur', roomId: 1, roomName: 'Salle Alpha', statut: 'OPERATIONNEL' },
      { id: 11, type: 'Visioconférence', roomId: 1, roomName: 'Salle Alpha', statut: 'EN_PANNE' },
    ],
  };

  type ComponentInternals = {
    form: {
      controls: { roomId: { setValue(value: number | null): void } };
      getRawValue(): unknown;
    };
    selectedEquipmentIds: () => number[];
    onRoomChange(roomId: number): void;
    toggleEquipment(equipmentId: number, checked: boolean): void;
    checkAvailability(): void;
    onSubmit(): void;
    createAnother(): void;
    createdBooking: () => BookingResponse | null;
    submitError: () => string | null;
  };

  function internals(fixture: { componentInstance: unknown }): ComponentInternals {
    return fixture.componentInstance as unknown as ComponentInternals;
  }

  beforeEach(() => {
    roomServiceStub = {
      listRooms: vi.fn(() => of([roomWithEquipment])),
      checkAvailability: vi.fn(),
    };
    bookingServiceStub = { createBooking: vi.fn() };
    snackBarStub = { open: vi.fn() };

    TestBed.configureTestingModule({
      imports: [BookingForm],
      providers: [
        provideRouter([]),
        { provide: RoomService, useValue: roomServiceStub },
        { provide: BookingService, useValue: bookingServiceStub },
        { provide: MatSnackBar, useValue: snackBarStub },
      ],
    });

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  it('loads the room list on init', () => {
    // Le contenu des mat-option n'est rendu dans le DOM principal qu'une fois le panneau ouvert
    // (portail CDK Overlay) : on vérifie l'état du composant plutôt que le texte affiché.
    const fixture = TestBed.createComponent(BookingForm);
    fixture.detectChanges();

    expect(roomServiceStub.listRooms).toHaveBeenCalled();
    expect(
      (fixture.componentInstance as unknown as { rooms: () => RoomResponse[] }).rooms(),
    ).toEqual([roomWithEquipment]);
  });

  it('resets the selected equipment when the room changes', () => {
    const fixture = TestBed.createComponent(BookingForm);
    fixture.detectChanges();
    const component = internals(fixture);

    component.onRoomChange(1);
    component.toggleEquipment(10, true);
    expect(component.selectedEquipmentIds()).toEqual([10]);

    component.onRoomChange(1);
    expect(component.selectedEquipmentIds()).toEqual([]);
  });

  it('toggleEquipment adds and removes equipment ids', () => {
    const fixture = TestBed.createComponent(BookingForm);
    fixture.detectChanges();
    const component = internals(fixture);

    component.toggleEquipment(10, true);
    component.toggleEquipment(11, true);
    expect(component.selectedEquipmentIds()).toEqual([10, 11]);

    component.toggleEquipment(10, false);
    expect(component.selectedEquipmentIds()).toEqual([11]);
  });

  it('checkAvailability forwards the selected equipment ids to the room service', () => {
    roomServiceStub.checkAvailability.mockReturnValue(
      of({
        roomId: 1,
        roomName: 'Salle Alpha',
        isAvailable: true,
        dateDebut: '2030-01-15T09:00:00.000Z',
        dateFin: '2030-01-15T10:00:00.000Z',
      } satisfies AvailabilityResponse),
    );
    const fixture = TestBed.createComponent(BookingForm);
    fixture.detectChanges();
    const component = internals(fixture);

    component.form.controls.roomId.setValue(1);
    component.toggleEquipment(10, true);
    component.checkAvailability();

    expect(roomServiceStub.checkAvailability).toHaveBeenCalledWith(
      1,
      expect.any(String),
      expect.any(String),
      [10],
    );
  });

  it('does not submit when the form is invalid (no room selected)', () => {
    const fixture = TestBed.createComponent(BookingForm);
    fixture.detectChanges();

    internals(fixture).onSubmit();

    expect(bookingServiceStub.createBooking).not.toHaveBeenCalled();
  });

  it('rejects an end time before or equal to the start time without calling the API', () => {
    const fixture = TestBed.createComponent(BookingForm);
    fixture.detectChanges();
    const component = internals(fixture);
    component.form.controls.roomId.setValue(1);
    (component.form as unknown as { patchValue(v: unknown): void }).patchValue({
      heureDebut: '10:00',
      heureFin: '09:00',
    });

    component.onSubmit();

    expect(bookingServiceStub.createBooking).not.toHaveBeenCalled();
    expect(component.submitError()).toContain('La date de fin doit être après la date de début.');
  });

  it('creates the booking with the selected equipment ids and shows the confirmation panel', () => {
    const created: BookingResponse = {
      id: 1,
      roomId: 1,
      roomName: 'Salle Alpha',
      userId: 5,
      userName: 'Jean Dupont',
      companyName: 'TechCorp',
      dateDebut: '2030-01-15T09:00:00.000Z',
      dateFin: '2030-01-15T10:00:00.000Z',
      statut: 'CONFIRMEE',
      version: 0,
      ecoScore: 50,
      equipmentsActives: [roomWithEquipment.equipements![0]],
    };
    bookingServiceStub.createBooking.mockReturnValue(of(created));
    const fixture = TestBed.createComponent(BookingForm);
    fixture.detectChanges();
    const component = internals(fixture);
    component.form.controls.roomId.setValue(1);
    component.toggleEquipment(10, true);

    component.onSubmit();
    fixture.detectChanges();

    expect(bookingServiceStub.createBooking).toHaveBeenCalledWith(
      expect.objectContaining({ roomId: 1, equipmentIds: [10] }),
    );
    expect(snackBarStub.open).toHaveBeenCalled();
    expect(component.createdBooking()).toEqual(created);
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Score éco-responsable');
  });

  it('maps a 409 conflict to a dedicated error message', () => {
    bookingServiceStub.createBooking.mockReturnValue(
      throwError(
        () => new HttpErrorResponse({ status: 409, error: { message: 'Créneau indisponible' } }),
      ),
    );
    const fixture = TestBed.createComponent(BookingForm);
    fixture.detectChanges();
    const component = internals(fixture);
    component.form.controls.roomId.setValue(1);

    component.onSubmit();

    expect(component.submitError()).toBe('Créneau indisponible');
  });

  it('createAnother clears the confirmation panel and resets the form', () => {
    const created: BookingResponse = {
      id: 1,
      roomId: 1,
      roomName: 'Salle Alpha',
      userId: 5,
      userName: 'Jean Dupont',
      companyName: 'TechCorp',
      dateDebut: '2030-01-15T09:00:00.000Z',
      dateFin: '2030-01-15T10:00:00.000Z',
      statut: 'CONFIRMEE',
      version: 0,
    };
    bookingServiceStub.createBooking.mockReturnValue(of(created));
    const fixture = TestBed.createComponent(BookingForm);
    fixture.detectChanges();
    const component = internals(fixture);
    component.form.controls.roomId.setValue(1);
    component.onSubmit();
    expect(component.createdBooking()).not.toBeNull();

    component.createAnother();

    expect(component.createdBooking()).toBeNull();
    expect(component.selectedEquipmentIds()).toEqual([]);
  });
});

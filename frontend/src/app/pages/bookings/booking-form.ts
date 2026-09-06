import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { combineDateAndTime, toDateInputValue } from '../../core/date.util';
import { describeApiError } from '../../core/http/error-message';
import { AvailabilityResponse, RoomResponse } from '../../core/models/room.models';
import { BookingResponse } from '../../core/models/booking.models';
import { RoomService } from '../../core/rooms/room.service';
import { BookingService } from '../../core/bookings/booking.service';

@Component({
  selector: 'app-booking-form',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './booking-form.html',
  styleUrl: './booking-form.scss',
})
export class BookingForm implements OnInit {
  private readonly formBuilder = new FormBuilder();

  protected readonly form = this.formBuilder.nonNullable.group({
    roomId: this.formBuilder.control<number | null>(null, Validators.required),
    date: [toDateInputValue(new Date()), Validators.required],
    heureDebut: ['09:00', Validators.required],
    heureFin: ['10:00', Validators.required],
    motif: [''],
  });

  protected readonly rooms = signal<RoomResponse[]>([]);
  protected readonly loadingRooms = signal(false);
  protected readonly submitting = signal(false);
  protected readonly submitError = signal<string | null>(null);
  protected readonly createdBooking = signal<BookingResponse | null>(null);

  protected readonly checkingAvailability = signal(false);
  protected readonly availability = signal<AvailabilityResponse | null>(null);

  // Équipements de la salle sélectionnée réellement sollicités pour cette réservation (choix
  // éco-responsable) : un équipement en panne ne bloque la réservation que s'il figure ici.
  protected readonly selectedRoomId = signal<number | null>(null);
  protected readonly selectedEquipmentIds = signal<number[]>([]);

  protected readonly roomEquipments = computed(
    () => this.rooms().find((room) => room.id === this.selectedRoomId())?.equipements ?? [],
  );

  constructor(
    private readonly roomService: RoomService,
    private readonly bookingService: BookingService,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.loadingRooms.set(true);
    this.roomService.listRooms().subscribe({
      next: (rooms) => {
        this.rooms.set(rooms);
        this.loadingRooms.set(false);
      },
      error: () => {
        this.loadingRooms.set(false);
      },
    });
  }

  protected onRoomChange(roomId: number): void {
    this.selectedRoomId.set(roomId);
    this.selectedEquipmentIds.set([]);
    this.availability.set(null);
  }

  protected toggleEquipment(equipmentId: number, checked: boolean): void {
    this.selectedEquipmentIds.update((ids) =>
      checked ? [...ids, equipmentId] : ids.filter((id) => id !== equipmentId),
    );
  }

  protected checkAvailability(): void {
    const { roomId, date, heureDebut, heureFin } = this.form.getRawValue();
    if (!roomId || !date || !heureDebut || !heureFin) {
      return;
    }

    const start = combineDateAndTime(date, heureDebut);
    const end = combineDateAndTime(date, heureFin);

    this.checkingAvailability.set(true);
    this.availability.set(null);
    this.roomService
      .checkAvailability(
        roomId,
        start.toISOString(),
        end.toISOString(),
        this.selectedEquipmentIds(),
      )
      .subscribe({
        next: (result) => {
          this.checkingAvailability.set(false);
          this.availability.set(result);
        },
        error: () => {
          this.checkingAvailability.set(false);
        },
      });
  }

  protected onSubmit(): void {
    this.submitError.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { roomId, date, heureDebut, heureFin, motif } = this.form.getRawValue();
    if (!roomId) {
      return;
    }

    const start = combineDateAndTime(date, heureDebut);
    const end = combineDateAndTime(date, heureFin);

    if (end <= start) {
      this.submitError.set('La date de fin doit être après la date de début.');
      return;
    }

    this.submitting.set(true);
    this.bookingService
      .createBooking({
        roomId,
        dateDebut: start.toISOString(),
        dateFin: end.toISOString(),
        motif: motif || undefined,
        equipmentIds: this.selectedEquipmentIds(),
      })
      .subscribe({
        next: (created) => {
          this.submitting.set(false);
          this.snackBar.open('Réservation confirmée.', 'Fermer', { duration: 4000 });
          this.createdBooking.set(created);
        },
        error: (error: unknown) => {
          this.submitting.set(false);
          this.submitError.set(
            describeApiError(error, {
              409: (apiError) =>
                apiError?.message ??
                'Cette salle est déjà réservée sur ce créneau. Choisissez un autre horaire.',
              400: (apiError) => apiError?.message ?? 'Données invalides.',
              403: "Vous n'êtes pas autorisé à créer de réservation.",
            }),
          );
        },
      });
  }

  protected goToBookings(): void {
    this.router.navigateByUrl('/reservations');
  }

  protected createAnother(): void {
    this.createdBooking.set(null);
    this.submitError.set(null);
    this.availability.set(null);
    this.selectedRoomId.set(null);
    this.selectedEquipmentIds.set([]);
    this.form.reset({
      roomId: null,
      date: toDateInputValue(new Date()),
      heureDebut: '09:00',
      heureFin: '10:00',
      motif: '',
    });
  }
}

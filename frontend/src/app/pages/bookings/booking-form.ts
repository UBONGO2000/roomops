import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { combineDateAndTime, toDateInputValue } from '../../core/date.util';
import { ApiError } from '../../core/models/error.models';
import { AvailabilityResponse, RoomResponse } from '../../core/models/room.models';
import { RoomService } from '../../core/rooms/room.service';
import { BookingService } from '../../core/bookings/booking.service';

@Component({
  selector: 'app-booking-form',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,
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

  protected readonly checkingAvailability = signal(false);
  protected readonly availability = signal<AvailabilityResponse | null>(null);

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

  protected checkAvailability(): void {
    const { roomId, date, heureDebut, heureFin } = this.form.getRawValue();
    if (!roomId || !date || !heureDebut || !heureFin) {
      return;
    }

    const start = combineDateAndTime(date, heureDebut);
    const end = combineDateAndTime(date, heureFin);

    this.checkingAvailability.set(true);
    this.availability.set(null);
    this.roomService.checkAvailability(roomId, start.toISOString(), end.toISOString()).subscribe({
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
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.snackBar.open('Réservation confirmée.', 'Fermer', { duration: 4000 });
          this.router.navigateByUrl('/reservations');
        },
        error: (error: unknown) => {
          this.submitting.set(false);
          this.submitError.set(this.describeSubmitError(error));
        },
      });
  }

  private describeSubmitError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const apiError = error.error as ApiError | undefined;
      if (error.status === 409) {
        return (
          apiError?.message ??
          'Cette salle est déjà réservée sur ce créneau. Choisissez un autre horaire.'
        );
      }
      if (error.status === 400) {
        return apiError?.message ?? 'Données invalides.';
      }
      if (error.status === 403) {
        return "Vous n'êtes pas autorisé à créer de réservation.";
      }
      if (error.status === 0) {
        return "Impossible de contacter le serveur. Vérifiez qu'il est démarré.";
      }
      if (apiError?.message) {
        return apiError.message;
      }
    }
    return 'Une erreur inattendue est survenue. Merci de réessayer.';
  }
}

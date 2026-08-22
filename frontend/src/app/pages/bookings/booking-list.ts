import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../core/auth/auth.service';
import { BookingService } from '../../core/bookings/booking.service';
import { BookingResponse } from '../../core/models/booking.models';
import { addDays, startOfWeek } from '../../core/date.util';

interface DayGroup {
  date: Date;
  bookings: BookingResponse[];
}

@Component({
  selector: 'app-booking-list',
  imports: [RouterLink, MatButtonModule, MatCardModule],
  templateUrl: './booking-list.html',
  styleUrl: './booking-list.scss',
})
export class BookingList {
  protected readonly weekStart = signal(startOfWeek(new Date()));
  protected readonly bookings = signal<BookingResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly cancellingId = signal<number | null>(null);

  protected readonly currentUser;

  protected readonly weekDays = computed(() => {
    const start = this.weekStart();
    return Array.from({ length: 7 }, (_, i) => addDays(start, i));
  });

  protected readonly dayGroups = computed<DayGroup[]>(() => {
    const bookingsByDay = new Map<string, BookingResponse[]>();
    for (const booking of this.bookings()) {
      const dayKey = booking.dateDebut.slice(0, 10);
      const group = bookingsByDay.get(dayKey) ?? [];
      group.push(booking);
      bookingsByDay.set(dayKey, group);
    }
    return this.weekDays().map((date) => ({
      date,
      bookings: (bookingsByDay.get(this.toIsoDate(date)) ?? []).sort((a, b) =>
        a.dateDebut.localeCompare(b.dateDebut),
      ),
    }));
  });

  constructor(
    private readonly bookingService: BookingService,
    authService: AuthService,
  ) {
    this.currentUser = authService.currentUser;
    this.loadWeek();
  }

  protected previousWeek(): void {
    this.weekStart.set(addDays(this.weekStart(), -7));
    this.loadWeek();
  }

  protected nextWeek(): void {
    this.weekStart.set(addDays(this.weekStart(), 7));
    this.loadWeek();
  }

  protected cancelBooking(booking: BookingResponse): void {
    const confirmed = window.confirm(
      `Annuler la réservation de "${booking.roomName}" le ${this.formatDayLabel(new Date(booking.dateDebut))} ?`,
    );
    if (!confirmed) {
      return;
    }

    this.cancellingId.set(booking.id);
    this.bookingService.cancelBooking(booking.id).subscribe({
      next: () => {
        this.cancellingId.set(null);
        this.loadWeek();
      },
      error: () => {
        this.cancellingId.set(null);
        this.errorMessage.set("Impossible d'annuler cette réservation pour le moment.");
      },
    });
  }

  protected formatDayLabel(date: Date): string {
    return date.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
  }

  protected formatTimeRange(booking: BookingResponse): string {
    const start = new Date(booking.dateDebut).toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
    });
    const end = new Date(booking.dateFin).toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
    });
    return `${start} – ${end}`;
  }

  protected formatWeekRangeLabel(): string {
    const start = this.weekStart();
    const end = addDays(start, 6);
    const format = (date: Date) =>
      date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
    return `${format(start)} – ${format(end)} ${end.getFullYear()}`;
  }

  private toIsoDate(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  private loadWeek(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    const start = this.weekStart();
    const end = addDays(start, 7);

    this.bookingService
      .listBookings({ dateDebut: start.toISOString(), dateFin: end.toISOString(), size: 100 })
      .subscribe({
        next: (page) => {
          this.bookings.set(page.content);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.errorMessage.set('Impossible de charger vos réservations pour le moment.');
        },
      });
  }
}

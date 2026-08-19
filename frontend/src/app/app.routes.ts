import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { Home } from './pages/home/home';
import { Login } from './pages/login/login';

export const routes: Routes = [
  { path: '', component: Home, title: 'RoomOps' },
  { path: 'connexion', component: Login, title: 'Se connecter — RoomOps' },
  {
    path: 'reservations',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/bookings/booking-list').then((m) => m.BookingList),
    title: 'Mes réservations — RoomOps',
  },
  {
    path: 'reservations/nouvelle',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/bookings/booking-form').then((m) => m.BookingForm),
    title: 'Nouvelle réservation — RoomOps',
  },
];

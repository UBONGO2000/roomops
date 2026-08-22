import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth/auth.guard';
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
  {
    path: 'equipements',
    canActivate: [authGuard, roleGuard(['SUPER_ADMIN'])],
    loadComponent: () => import('./pages/equipments/equipment-list').then((m) => m.EquipmentList),
    title: 'Équipements — RoomOps',
  },
  {
    path: 'profil',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/profile/profile').then((m) => m.Profile),
    title: 'Mon profil — RoomOps',
  },
  {
    path: 'employes',
    canActivate: [authGuard, roleGuard(['MANAGER'])],
    loadComponent: () => import('./pages/employees/employee-list').then((m) => m.EmployeeList),
    title: 'Mes employés — RoomOps',
  },
];

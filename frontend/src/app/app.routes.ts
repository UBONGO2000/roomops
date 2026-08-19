import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { Login } from './pages/login/login';

export const routes: Routes = [
  { path: '', component: Home, title: 'RoomOps' },
  { path: 'connexion', component: Login, title: 'Se connecter — RoomOps' },
];

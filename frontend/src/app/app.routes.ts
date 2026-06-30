import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/home/home').then((module) => module.Home),
    title: 'AgroScan | Comprendre vos cultures',
  },
  {
    path: 'faq',
    loadComponent: () => import('./pages/faq/faq').then((module) => module.Faq),
    title: 'FAQ | AgroScan',
  },
  {
    path: 'connexion',
    loadComponent: () => import('./pages/auth/auth').then((module) => module.Auth),
    title: 'Connexion et inscription | AgroScan',
  },
  {
    path: 'profil',
    loadComponent: () => import('./pages/profile/profile').then((module) => module.Profile),
    canActivate: [authGuard],
    title: 'Mon profil | AgroScan',
  },
  {
    path: 'mes-scans',
    loadComponent: () => import('./pages/scans/scans').then((module) => module.Scans),
    canActivate: [authGuard],
    title: 'Mes scans | AgroScan',
  },
  {
    path: 'utilisateurs',
    loadComponent: () =>
      import('./pages/admin-users/admin-users').then((module) => module.AdminUsers),
    canActivate: [adminGuard],
    title: 'Utilisateurs | AgroScan',
  },
  { path: '**', redirectTo: '' },
];

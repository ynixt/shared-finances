import { Routes } from '@angular/router';

import { authGuard } from './guards/auth.guard';
import { notLoggedGuard } from './guards/not-logged.guard';

export const routes: Routes = [
  {
    'path': '',
    canActivate: [notLoggedGuard()],
    loadComponent: () => import('./pages/showcase-page/showcase-page.component').then(m => m.ShowcasePageComponent),
  },
  {
    'path': 'login',
    canActivate: [notLoggedGuard()],
    loadComponent: () => import('./pages/login-page/login-page.component').then(m => m.LoginPageComponent),
  },
  {
    'path': 'register',
    canActivate: [notLoggedGuard()],
    loadComponent: () => import('./pages/registration-page/registration-page.component').then(m => m.RegistrationPageComponent),
  },
  {
    'path': 'dashboard',
    canActivate: [authGuard()],
    loadComponent: () => import('./pages/finances/finances-home-page/finances-home-page.component').then(m => m.FinancesHomePageComponent),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/finances/finances-overview-page/finances-overview-page.component').then(m => m.FinancesOverviewPageComponent),
      },
    ],
  },
  {
    'path': 'not-found',
    loadComponent: () => import('./pages/not-found-page/not-found-page.component').then(m => m.NotFoundPageComponent),
  },
  {
    path: '**',
    redirectTo: 'not-found',
  },
];

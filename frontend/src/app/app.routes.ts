import { Routes } from '@angular/router';

import { authGuard } from './guards/auth.guard';
import { notLoggedGuard } from './guards/not-logged.guard';

export const routes: Routes = [
  {
    'path': '',
    canActivate: [notLoggedGuard()],
    loadComponent: () => import('./pages/showcase-page/showcase-page.component').then(m => m.ShowcasePageComponent),
    data: {
      pageTitleKey: 'pageTitle.home',
    },
  },
  {
    'path': 'login',
    canActivate: [notLoggedGuard()],
    loadComponent: () => import('./pages/login-page/login-page.component').then(m => m.LoginPageComponent),
    data: {
      pageTitleKey: 'pageTitle.login',
    },
  },
  {
    'path': 'register',
    canActivate: [notLoggedGuard()],
    loadComponent: () => import('./pages/registration-page/registration-page.component').then(m => m.RegistrationPageComponent),
    data: {
      pageTitleKey: 'pageTitle.register',
    },
  },
  {
    path: 'welcome',
    canActivate: [authGuard()],
    loadComponent: () => import('./pages/onboarding-page/onboarding-page.component').then(m => m.OnboardingPageComponent),
    data: {
      pageTitleKey: 'pageTitle.onboarding',
    },
  },
  {
    'path': 'app',
    canActivate: [authGuard()],
    loadComponent: () => import('./pages/finances/finances-home-page/finances-page.component').then(m => m.FinancesPageComponent),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/finances/finances-overview-page/finances-overview-page.component').then(m => m.FinancesOverviewPageComponent),
        data: {
          pageTitleKey: 'pageTitle.app',
        },
      },
      {
        path: 'bankAccounts',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./pages/finances/finances-bank-accounts-page/finances-bank-accounts-page.component').then(
                m => m.FinancesBankAccountsPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.bankAccounts',
            },
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./pages/finances/finances-bank-accounts-page/new-bank-account-page/new-bank-account-page.component').then(
                m => m.NewBankAccountPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.newBankAccount',
            },
          },
          {
            path: 'edit/:id',
            loadComponent: () =>
              import('./pages/finances/finances-bank-accounts-page/edit-bank-account-page/edit-bank-account-page.component').then(
                m => m.EditBankAccountPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.editBankAccount',
            },
          },
        ],
      },
      {
        path: 'groups',
        children: [
          {
            path: '',
            redirectTo: '/app',
            pathMatch: 'full',
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./pages/finances/groups-page/new-group-page/new-group-page.component').then(m => m.NewGroupPageComponent),
            data: {
              pageTitleKey: 'pageTitle.newGroup',
            },
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./pages/finances/groups-page/overview-group-page/overview-group-page.component').then(
                m => m.OverviewGroupPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.overviewGroup',
            },
          },
          {
            path: ':id/team',
            loadComponent: () =>
              import('./pages/finances/groups-page/manage-group-team-page/manage-group-team-page.component').then(
                m => m.ManageGroupTeamPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.manageGroupTeam',
            },
          },
        ],
      },
    ],
  },
  {
    'path': 'not-found',
    loadComponent: () => import('./pages/not-found-page/not-found-page.component').then(m => m.NotFoundPageComponent),
    data: {
      pageTitleKey: 'pageTitle.notFound',
    },
  },
  {
    path: '**',
    redirectTo: 'not-found',
  },
];

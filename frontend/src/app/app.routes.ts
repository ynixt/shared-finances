import { Routes } from '@angular/router';

import { financesRoute } from './finances.routes';
import { authGuard } from './guards/auth.guard';
import { notLoggedGuard } from './guards/not-logged.guard';
import {
  emailConfirmationFlowsEnabledGuard,
  legalDocumentsEnabledGuard,
  passwordRecoveryEnabledGuard,
  publicPlanComparisonEnabledGuard,
  registrationEnabledGuard,
} from './guards/open-auth-feature.guards';

export const routes: Routes = [
  {
    'path': '',
    canActivate: [notLoggedGuard],
    loadComponent: () => import('./pages/showcase-page/showcase-page.component').then(m => m.ShowcasePageComponent),
    data: {
      pageTitleKey: 'pageTitle.home',
    },
  },
  {
    'path': 'login',
    canActivate: [notLoggedGuard],
    loadComponent: () => import('./pages/login-page/login-page.component').then(m => m.LoginPageComponent),
    data: {
      pageTitleKey: 'pageTitle.login',
    },
  },
  {
    'path': 'register',
    canActivate: [notLoggedGuard],
    canMatch: [registrationEnabledGuard],
    loadComponent: () => import('./pages/registration-page/registration-page.component').then(m => m.RegistrationPageComponent),
    data: {
      pageTitleKey: 'pageTitle.register',
    },
  },
  {
    path: 'pending-email-confirmation',
    canActivate: [notLoggedGuard],
    canMatch: [emailConfirmationFlowsEnabledGuard],
    loadComponent: () =>
      import('./pages/pending-email-confirmation-page/pending-email-confirmation-page.component').then(
        m => m.PendingEmailConfirmationPageComponent,
      ),
    data: { pageTitleKey: 'pageTitle.pendingEmail' },
  },
  {
    path: 'confirm-email',
    canActivate: [notLoggedGuard],
    canMatch: [emailConfirmationFlowsEnabledGuard],
    loadComponent: () => import('./pages/confirm-email-page/confirm-email-page.component').then(m => m.ConfirmEmailPageComponent),
    data: { pageTitleKey: 'pageTitle.confirmEmail' },
  },
  {
    path: 'forgot-password',
    canActivate: [notLoggedGuard],
    canMatch: [passwordRecoveryEnabledGuard],
    loadComponent: () => import('./pages/forgot-password-page/forgot-password-page.component').then(m => m.ForgotPasswordPageComponent),
    data: { pageTitleKey: 'pageTitle.forgotPassword' },
  },
  {
    path: 'reset-password',
    canActivate: [notLoggedGuard],
    canMatch: [passwordRecoveryEnabledGuard],
    loadComponent: () => import('./pages/reset-password-page/reset-password-page.component').then(m => m.ResetPasswordPageComponent),
    data: { pageTitleKey: 'pageTitle.resetPassword' },
  },
  {
    path: 'legal/terms',
    canMatch: [legalDocumentsEnabledGuard],
    loadComponent: () => import('./pages/legal/legal-document-page.component').then(m => m.LegalDocumentPageComponent),
    data: {
      pageTitleKey: 'pageTitle.legalTerms',
      legalDoc: 'terms',
    },
  },
  {
    path: 'legal/privacy',
    canMatch: [legalDocumentsEnabledGuard],
    loadComponent: () => import('./pages/legal/legal-document-page.component').then(m => m.LegalDocumentPageComponent),
    data: {
      pageTitleKey: 'pageTitle.legalPrivacy',
      legalDoc: 'privacy',
    },
  },
  {
    path: 'plans',
    canMatch: [publicPlanComparisonEnabledGuard],
    loadComponent: () => import('./pages/plans/plans-page.component').then(m => m.PlansPageComponent),
    data: {
      pageTitleKey: 'pageTitle.plans',
    },
  },
  {
    path: 'welcome',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/onboarding-page/onboarding-page.component').then(m => m.OnboardingPageComponent),
    data: {
      pageTitleKey: 'pageTitle.onboarding',
    },
  },
  financesRoute,
  {
    path: 'invite/:id',
    loadComponent: () => import('./pages/accept-invite-page/accept-invite-page.component').then(m => m.AcceptInvitePageComponent),
    data: {
      pageTitleKey: 'pageTitle.acceptInvite',
    },
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

import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';

import { OpenAuthPreferencesService } from '../services/open-auth-preferences.service';

/** Public registration route when new account creation is disabled server-side. */
export const registrationEnabledGuard: CanMatchFn = async () => {
  const prefs = inject(OpenAuthPreferencesService);
  const router = inject(Router);
  await prefs.load();
  if (prefs.preferences()?.registrationEnabled === false) {
    return router.createUrlTree(['/login']);
  }
  return true;
};

/** Forgot / reset password routes when recovery is disabled server-side. */
export const passwordRecoveryEnabledGuard: CanMatchFn = async () => {
  const prefs = inject(OpenAuthPreferencesService);
  const router = inject(Router);
  await prefs.load();
  if (prefs.preferences()?.passwordRecoveryEnabled === false) {
    return router.createUrlTree(['/login']);
  }
  return true;
};

/** Email confirmation UX routes when confirmation is disabled server-side. */
export const emailConfirmationFlowsEnabledGuard: CanMatchFn = async () => {
  const prefs = inject(OpenAuthPreferencesService);
  const router = inject(Router);
  await prefs.load();
  if (prefs.preferences()?.emailConfirmationEnabled === false) {
    return router.createUrlTree(['/login']);
  }
  return true;
};

const publicFeatureGuard = async (feature: 'legalDocumentsEnabled' | 'planLimitsEnabled') => {
  const prefs = inject(OpenAuthPreferencesService);
  const router = inject(Router);
  await prefs.load();
  return prefs.preferences()?.[feature] === true || router.createUrlTree(['/not-found']);
};

/** Terms and privacy routes exist only on instances that present their documents. */
export const legalDocumentsEnabledGuard: CanMatchFn = () => publicFeatureGuard('legalDocumentsEnabled');

/** The public comparison exists only while the instance enforces its plan model. */
export const publicPlanComparisonEnabledGuard: CanMatchFn = () => publicFeatureGuard('planLimitsEnabled');

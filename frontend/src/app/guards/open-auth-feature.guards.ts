import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';

import { OpenAuthPreferencesService } from '../services/open-auth-preferences.service';

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

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { PlanEntitlementsStore } from '../services/plan-entitlements.store';

export const planLimitsEnabledGuard: CanActivateFn = async () => {
  const entitlements = inject(PlanEntitlementsStore);
  const router = inject(Router);

  if (entitlements.entitlements() == null) {
    await entitlements.reload();
  }

  return entitlements.limitsEnabled() || router.createUrlTree(['/app']);
};

import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { KratosAuthService } from '../services/kratos-auth.service';

export const notLoggedGuard = (): CanActivateFn => {
  return async (route, state): Promise<boolean | UrlTree> => {
    const authService = inject(KratosAuthService);
    const router = inject(Router);

    try {
      const token = await authService.getToken();

      if (token == null) {
        return true;
      } else {
        return router.createUrlTree(['dashboard']);
      }
    } catch (err) {
      return true;
    }
  };
};

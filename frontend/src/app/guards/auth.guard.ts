import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { KratosAuthService } from '../services/kratos-auth.service';

export const authGuard = (): CanActivateFn => {
  return async (route, state): Promise<boolean | UrlTree> => {
    const authService = inject(KratosAuthService);
    const router = inject(Router);

    try {
      const token = await authService.getToken();
      return token != null;
    } catch (error) {
      console.error(error);
      const url = router.createUrlTree(['/login'], {
        queryParams: { return_to: window.location.pathname },
      });
      return url;
    }
  };
};

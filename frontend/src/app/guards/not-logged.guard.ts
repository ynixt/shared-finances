import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { UserService } from '../services/user.service';

export const notLoggedGuard = (): CanActivateFn => {
  return async (route, state): Promise<boolean | UrlTree> => {
    const userService = inject(UserService);
    const router = inject(Router);

    try {
      const user = await userService.getUser();

      if (user == null) {
        return true;
      } else {
        return router.createUrlTree(['dashboard']);
      }
    } catch (err) {
      return true;
    }
  };
};

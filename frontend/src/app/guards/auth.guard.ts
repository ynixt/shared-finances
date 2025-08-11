import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { UserService } from '../services/user.service';

export const authGuard = (): CanActivateFn => {
  return async (route, state): Promise<boolean | UrlTree> => {
    const userService = inject(UserService);
    const router = inject(Router);

    try {
      const user = await userService.getUser();
      return user != null;
    } catch (error) {
      console.error(error);
      const url = router.createUrlTree(['/login'], {
        queryParams: { return_to: window.location.pathname },
      });
      return url;
    }
  };
};

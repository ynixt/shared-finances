import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { UserService } from '../services/user.service';

export const authGuard: CanActivateFn = async (route, state) => {
  const userService = inject(UserService);
  const router = inject(Router);
  const returnTo = state.url?.length > 0 ? state.url : `${window.location.pathname}${window.location.search}`;

  try {
    const user = await userService.getUser();
    if (user != null) {
      return true;
    }

    return router.createUrlTree(['/login'], {
      queryParams: { return_to: returnTo },
    });
  } catch (error) {
    console.error(error);
    return router.createUrlTree(['/login'], {
      queryParams: { return_to: returnTo },
    });
  }
};

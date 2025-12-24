import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { UserService } from '../services/user.service';

export const notLoggedGuard: CanActivateFn = async (route, state) => {
  const userService = inject(UserService);
  const router = inject(Router);

  try {
    const user = await userService.getUser();

    if (user == null) {
      return true;
    } else {
      return router.createUrlTree(['app']);
    }
  } catch (err) {
    return true;
  }
};

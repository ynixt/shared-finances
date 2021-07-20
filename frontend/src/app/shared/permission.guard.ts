import { Injectable } from '@angular/core';
import { CanActivate, CanActivateChild, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthSelectors } from '../store/services/selectors';

@Injectable({
  providedIn: 'root',
})
export class PermissionGuard implements CanActivate, CanActivateChild {
  private permissionByRoute = new Map<string, string>();

  public constructor(private authSelector: AuthSelectors, private router: Router) {}

  public async canActivate(route: ActivatedRouteSnapshot): Promise<boolean> {
    const user = await this.authSelector.currentUser();

    if (user) {
      const permissions = user.permissions || [];
      const routeUrl: string = route.url.map(segment => segment.path).join('/');
      const necessaryPermission = this.permissionByRoute.get(routeUrl);

      if (
        !this.permissionByRoute.has(routeUrl) ||
        (this.permissionByRoute.has(routeUrl) && permissions.map(permission => permission.code).includes(<string>necessaryPermission))
      ) {
        return true;
      }

      this.router.navigateByUrl('/auth/login');
      return false;
    }

    this.router.navigateByUrl(`/auth/login?next=${window.location.pathname}`);

    return false;
  }

  public canActivateChild(
    childRoute: ActivatedRouteSnapshot,
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    return this.canActivate(childRoute);
  }
}

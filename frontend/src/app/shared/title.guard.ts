import { Injectable } from '@angular/core';
import { CanActivate, CanActivateChild, ActivatedRouteSnapshot } from '@angular/router';
import { TitleService } from '../@core/services';

@Injectable({
  providedIn: 'root',
})
export class TitleGuard implements CanActivate {
  private permissionByRoute = new Map<string, string>();

  public constructor(private titleService: TitleService) {
    this.permissionByRoute.set('', 'view_dashboard');
  }

  public async canActivate(route: ActivatedRouteSnapshot): Promise<boolean> {
    await this.changeTitle(route);
    return true;
  }

  private async changeTitle(route: ActivatedRouteSnapshot): Promise<void> {
    const title: string = route.data.title;

    await this.titleService.changeTitle(title);
  }
}

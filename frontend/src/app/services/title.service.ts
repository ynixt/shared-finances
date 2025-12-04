import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

@Injectable({ providedIn: 'root' })
export class TitleService {
  constructor(private translateService: TranslateService) {}

  getTitle(route: ActivatedRouteSnapshot, fullRoutePath: string): string {
    let fullPath = '';
    while (route.firstChild) {
      route = route.firstChild;
      if (route.routeConfig?.path) {
        fullPath += '/' + route.routeConfig.path;
      }
    }

    fullPath = fullPath.replace(/\/\/+/g, '/');

    if (!fullPath) {
      fullPath = fullRoutePath;
    }

    return this.mountTitle(fullPath, route.data['pageTitleKey']);
  }

  private mountTitle(route: string, titleFromRouteData: string | undefined): string {
    return this.translateService.instant(titleFromRouteData ?? `pageTitleByUrl.${route}\``);
  }
}

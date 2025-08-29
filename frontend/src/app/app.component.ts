import { HttpClient } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { filter } from 'rxjs';

import { ButtonModule } from 'primeng/button';
import { PrimeNG } from 'primeng/config';

import { TitleService } from './services/title.service';
import { UserService } from './services/user.service';
import { i18nIsReady } from './util/i18n-util';
import { updatePrimeI18n } from './util/prime-i18n';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ButtonModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: ` <router-outlet />`,
  styles: [],
})
export class AppComponent {
  constructor(
    private primengConfig: PrimeNG,
    private translateService: TranslateService,
    private httpClient: HttpClient,
    private userService: UserService,
    private title: Title,
    private titleService: TitleService,
    private router: Router,
  ) {
    updatePrimeI18n(this.primengConfig, this.translateService, this.httpClient);

    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(async () => {
      await i18nIsReady(this.translateService);
      this.title.setTitle(this.titleService.getTitle(this.router.routerState.snapshot.root, this.router.url));
    });

    this.userService.getUser();
  }
}

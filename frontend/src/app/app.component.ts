import { HttpClient } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { filter, interval } from 'rxjs';

import { ButtonModule } from 'primeng/button';
import { PrimeNG } from 'primeng/config';

import { KratosAuthService } from './services/kratos-auth.service';
import { LangService } from './services/lang.service';
import { TitleService } from './services/title.service';
import { TokenSyncService } from './services/token-sync.service';
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
  private readonly timeToRefreshToken = 9 * 60 * 1000;

  constructor(
    private primengConfig: PrimeNG,
    private translateService: TranslateService,
    private httpClient: HttpClient,
    private title: Title,
    private titleService: TitleService,
    private router: Router,
    private tokenSyncService: TokenSyncService,
    private userService: UserService,
    private authService: KratosAuthService,
    private langService: LangService,
  ) {
    this.langService.init();

    updatePrimeI18n(this.primengConfig, this.translateService, this.httpClient);

    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(async () => {
      await i18nIsReady(this.translateService);
      this.title.setTitle(this.titleService.getTitle(this.router.routerState.snapshot.root, this.router.url));
    });

    this.authService.refreshJwt();
    this.userService.getUser();

    interval(this.timeToRefreshToken).subscribe(() => {
      this.tokenSyncService.refreshOnce();
    });
  }
}

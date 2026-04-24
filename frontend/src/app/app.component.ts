import { HttpClient } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { filter, interval } from 'rxjs';

import { ButtonModule } from 'primeng/button';
import { PrimeNG } from 'primeng/config';

import { FooterComponent } from './components/footer/footer.component';
import { AuthService } from './services/auth.service';
import { LangService } from './services/lang.service';
import { TitleService } from './services/title.service';
import { UserService } from './services/user.service';
import { i18nIsReady } from './util/i18n-util';
import { updatePrimeI18n } from './util/prime-i18n';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ButtonModule, FooterComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <div class="w-full  flex flex-1 router-wrapper-full">
      <router-outlet />
    </div>
    <app-footer></app-footer>
  `,
  styles: [],
})
export class AppComponent {
  constructor(
    private primengConfig: PrimeNG,
    private translateService: TranslateService,
    private httpClient: HttpClient,
    private title: Title,
    private titleService: TitleService,
    private router: Router,
    private userService: UserService,
    private authService: AuthService,
    private langService: LangService,
  ) {
    this.langService.init();

    updatePrimeI18n(this.primengConfig, this.translateService, this.httpClient);

    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(async () => {
      await i18nIsReady(this.translateService);
      this.title.setTitle(this.titleService.getTitle(this.router.routerState.snapshot.root, this.router.url));
    });

    this.userService.getUser();
  }
}

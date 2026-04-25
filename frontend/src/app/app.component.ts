import { HttpClient } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA, Component, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslateService } from '@ngx-translate/core';

import { filter, interval } from 'rxjs';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { PrimeNG } from 'primeng/config';
import { ProgressSpinner } from 'primeng/progressspinner';
import { Toast } from 'primeng/toast';

import { FooterComponent } from './components/footer/footer.component';
import { UserResponseDto } from './models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { ActionEventService } from './pages/finances/services/action-event.service';
import { UserActionEventService } from './pages/finances/services/user-action-event.service';
import { AuthService } from './services/auth.service';
import { DarkModeService } from './services/dark-mode.service';
import { LangService } from './services/lang.service';
import { TitleService } from './services/title.service';
import { UserService } from './services/user.service';
import { DEFAULT_ERROR_LIFE } from './util/error-util';
import { i18nIsReady } from './util/i18n-util';
import { updatePrimeI18n } from './util/prime-i18n';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ButtonModule, FooterComponent, ProgressSpinner, Toast],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './app.component.html',
  styles: [],
  providers: [MessageService],
})
@UntilDestroy()
export class AppComponent {
  private user: UserResponseDto | undefined | null;

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
    private messageService: MessageService,
    private userActionEventService: UserActionEventService,
    private darkModeService: DarkModeService,
  ) {
    this.langService.init();

    void updatePrimeI18n(this.primengConfig, this.translateService, this.httpClient);

    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(async () => {
      await i18nIsReady(this.translateService);
      this.title.setTitle(this.titleService.getTitle(this.router.routerState.snapshot.root, this.router.url));
    });

    this.userService.user$.pipe(untilDestroyed(this)).subscribe(u => (this.user = u));

    this.authService.onServerOffline$.pipe(untilDestroyed(this)).subscribe(() => {
      this.messageService.add({
        severity: 'error',
        summary: this.translateService.instant('error.genericTitle'),
        detail: this.translateService.instant('error.serverMessage'),
        closable: false,
        sticky: true,
      });
    });

    this.userActionEventService.userUpdated$.pipe(untilDestroyed(this)).subscribe(newData => {
      const currentUser = this.user;

      if (currentUser == null) return;

      if (currentUser.darkMode != newData.darkMode) {
        this.darkModeService.setDarkMode(newData.darkMode);
      }

      if (currentUser.lang != newData.lang) {
        void this.langService.changeLanguage(newData.lang, false);
      }

      this.userService.changeUser(newData);
    });
  }
}

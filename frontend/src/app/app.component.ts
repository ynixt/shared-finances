import { HttpClient } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { ButtonModule } from 'primeng/button';
import { PrimeNG } from 'primeng/config';

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
  ) {
    updatePrimeI18n(this.primengConfig, this.translateService, this.httpClient);
  }
}

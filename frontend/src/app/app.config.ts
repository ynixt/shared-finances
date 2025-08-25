import { HTTP_INTERCEPTORS, HttpBackend, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { ApplicationConfig, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { providePrimeNG } from 'primeng/config';

import { environment } from '../environments/environment';
import { SharedFinancesPreset } from '../theme-preset';
import { routes } from './app.routes';
import { CustomTranslateYamlLoader } from './custom-translate-yaml-loader';
import { AuthInterceptor } from './interceptors/auth.interceptor';

const httpLoaderFactory = (httpBackend: HttpBackend): CustomTranslateYamlLoader => new CustomTranslateYamlLoader();

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: SharedFinancesPreset,
        options: {
          darkModeSelector: '.dark-mode',
          cssLayer: {
            name: 'primeng',
            order: 'theme, base, primeng, app-styles',
          },
        },
      },
    }),
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    importProvidersFrom(
      BrowserAnimationsModule,
      TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useFactory: httpLoaderFactory,
          deps: [HttpBackend],
        },
        defaultLanguage: environment.defaultLanguage,
        useDefaultLang: true,
      }),
    ),
  ],
};

import { HTTP_INTERCEPTORS, HttpBackend, provideHttpClient, withInterceptors, withInterceptorsFromDi } from '@angular/common/http';
import { APP_INITIALIZER, ApplicationConfig, importProvidersFrom, inject, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { InMemoryCache } from '@apollo/client/core';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { provideApollo } from 'apollo-angular';
import { HttpLink } from 'apollo-angular/http';
import { ConfirmationService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';

import { environment } from '../environments/environment';
import { SharedFinancesPreset } from '../theme-preset';
import { routes } from './app.routes';
import { CustomTranslateYamlLoader } from './custom-translate-yaml-loader';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { apiAuthInterceptor } from './interceptors/unauthorized.interceptor';
import { OpenAuthPreferencesService } from './services/open-auth-preferences.service';

const httpLoaderFactory = (httpBackend: HttpBackend): CustomTranslateYamlLoader => new CustomTranslateYamlLoader();

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi(), withInterceptors([apiAuthInterceptor])),
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: (openAuthPreferences: OpenAuthPreferencesService) => () => openAuthPreferences.load(),
      deps: [OpenAuthPreferencesService],
    },
    ConfirmationService,
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
    provideApollo(() => {
      const httpLink = inject(HttpLink);

      return {
        link: httpLink.create({ uri: '/api/graphql' }),
        cache: new InMemoryCache(),
      };
    }),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    importProvidersFrom(
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

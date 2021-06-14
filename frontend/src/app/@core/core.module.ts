import { LOCALE_ID, NgModule } from '@angular/core';
import { CommonModule, getCurrencySymbol, getLocaleCurrencyCode } from '@angular/common';
import { AngularFireModule } from '@angular/fire';
import { AngularFireAuthModule } from '@angular/fire/auth';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { CurrencyMaskConfig, CURRENCY_MASK_CONFIG } from 'ng2-currency-mask';
import localePt from '@angular/common/locales/pt';

import { TokenInterceptor } from './interceptors/token.interceptor';
import { AppStoreModule } from '../store/app-store.module';
import { TranslocoRootModule } from './i18n';
import { GraphQLModule } from './graphql.module';

export function createCurrencyMaskConfig(): CurrencyMaskConfig {
  const currencySymbol = getCurrencySymbol(getLocaleCurrencyCode('pt'), 'narrow');

  const decimalSeparator = (localePt[13] as string[])[0];
  const thousdandSeparator = (localePt[13] as string[])[1];

  return {
    align: 'left',
    allowNegative: false,
    decimal: decimalSeparator,
    precision: 2,
    prefix: `${currencySymbol} `,
    suffix: '',
    thousands: thousdandSeparator,
  };
}

import { registerLocaleData } from '@angular/common';
import { ErrorService } from './services/error.service';
import { MatMomentDateModule, MAT_MOMENT_DATE_ADAPTER_OPTIONS } from '@angular/material-moment-adapter';
import { MAT_DATE_FORMATS } from '@angular/material/core';
import { UserCategoryService } from './services/user-category.service';

registerLocaleData(localePt, 'pt');

@NgModule({
  imports: [
    CommonModule,
    TranslocoRootModule,
    AngularFireModule.initializeApp({
      apiKey: 'AIzaSyAU6zY3ftSLQ_cAfSxqWppzBbnAFFjCfSs',
      authDomain: 'unk-shared-finances.firebaseapp.com',
      projectId: 'unk-shared-finances',
      storageBucket: 'unk-shared-finances.appspot.com',
      messagingSenderId: '6480837718',
      appId: '1:6480837718:web:37e872781c65d2342e4ff7',
      measurementId: 'G-F9RVQCE689',
    }),
    AngularFireAuthModule,
    AppStoreModule,
    GraphQLModule,
    MatMomentDateModule,
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: TokenInterceptor,
      multi: true,
    },
    { provide: LOCALE_ID, useValue: 'pt' },
    { provide: CURRENCY_MASK_CONFIG, useFactory: createCurrencyMaskConfig },
    { provide: MAT_MOMENT_DATE_ADAPTER_OPTIONS, useValue: { strict: true } },
    {
      provide: MAT_DATE_FORMATS,
      useValue: {
        parse: {
          dateInput: ['l', 'LL'],
        },
        display: {
          dateInput: 'L',
          monthYearLabel: 'MMM YYYY',
          dateA11yLabel: 'LL',
          monthYearA11yLabel: 'MMMM YYYY',
        },
      },
    },
    ErrorService,
    UserCategoryService,
  ],
  exports: [TranslocoRootModule],
})
export class CoreModule {}

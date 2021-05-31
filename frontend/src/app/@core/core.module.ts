import { LOCALE_ID, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AngularFireModule } from '@angular/fire';
import { AngularFireAuthModule } from '@angular/fire/auth';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { TokenInterceptor } from './interceptors/token.interceptor';
import { AppStoreModule } from '../store/app-store.module';
import { TranslocoRootModule } from './i18n';
import { GraphQLModule } from './graphql.module';

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
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: TokenInterceptor,
      multi: true,
    },
    [{ provide: LOCALE_ID, useValue: 'pt' }],
  ],
  exports: [TranslocoRootModule],
})
export class CoreModule {}

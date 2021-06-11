import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { CoreModule } from './@core/core.module';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { HeaderModule } from './components/layout';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ErrorsModule } from './pages/errors';
import { HotToastModule } from '@ngneat/hot-toast';
import { NewTransactionModule } from './components/new-transaction/new-transaction.module';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    CoreModule,
    BrowserAnimationsModule,
    HeaderModule,
    MatProgressSpinnerModule,
    ErrorsModule,
    HotToastModule.forRoot({
      position: 'bottom-center',
    }),
    NewTransactionModule,
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}

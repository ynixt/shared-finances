import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslocoModule } from '@ngneat/transloco';

import { HeaderComponent } from './header.component';
import { CovalentStepsModule } from '@covalent/core/steps';
import { CovalentLayoutModule } from '@covalent/core/layout';
import { MatListModule } from '@angular/material/list';
import { MenuModule } from '../menu';
import { MatTooltipModule } from '@angular/material/tooltip';

@NgModule({
  declarations: [HeaderComponent],
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    MatDividerModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    TranslocoModule,
    CovalentLayoutModule,
    CovalentStepsModule,
    MatListModule,
    MenuModule,
    MatTooltipModule,
  ],
  exports: [HeaderComponent],
})
export class HeaderModule {}

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ScrollingModule } from '@angular/cdk/scrolling';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';

import { CovalentLayoutModule } from '@covalent/core/layout';
import { CovalentStepsModule } from '@covalent/core/steps';
import { TranslocoModule } from '@ngneat/transloco';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

const layoutModules = [
  ScrollingModule,
  MatSidenavModule,
  MatToolbarModule,
  MatButtonModule,
  MatIconModule,
  MatCardModule,
  MatDividerModule,
  MatListModule,
  MatProgressSpinnerModule,

  CovalentLayoutModule,
  CovalentStepsModule,
  TranslocoModule,
];

@NgModule({
  declarations: [],
  imports: [CommonModule, ...layoutModules],
  exports: [...layoutModules],
})
export class SharedLibsModule {}

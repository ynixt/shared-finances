import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';


import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CovalentLayoutModule } from '@covalent/core/layout';
import { CovalentBreadcrumbsModule } from '@covalent/core/breadcrumbs';
// import { CovalentStepsModule } from '@covalent/core/steps';
import { TranslocoModule } from '@ngneat/transloco';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ScrollingModule } from "@angular/cdk/scrolling";
import { MatPaginatorIntl, MatPaginatorModule } from "@angular/material/paginator";
import { ApolloModule } from "apollo-angular";
import { MatButtonModule } from "@angular/material/button";
import { MatLineModule } from "@angular/material/core";
import { ShPaginatorIntl } from "../sh-paginator-intl";

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
  MatTooltipModule,
  MatPaginatorModule,
  MatLineModule,
  ApolloModule,

  CovalentBreadcrumbsModule,
  CovalentLayoutModule,
  // CovalentStepsModule,

  TranslocoModule,
];

@NgModule({
  declarations: [],
  providers: [
    { provide: MatPaginatorIntl, useClass: ShPaginatorIntl },
  ],
  imports: [CommonModule, ...layoutModules],
  exports: [...layoutModules],
})
export class SharedLibsModule {}

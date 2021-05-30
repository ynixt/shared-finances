import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { FinancesRoutingModule } from './finances-routing.module';
import { DashboardComponent } from './dashboard/dashboard.component';
import { FinancesComponent } from './finances.component';
import { SharedModule } from 'src/app/shared/shared.module';

@NgModule({
  declarations: [DashboardComponent, FinancesComponent],
  imports: [CommonModule, SharedModule, FinancesRoutingModule],
})
export class FinancesModule {}

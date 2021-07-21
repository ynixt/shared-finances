import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { FinancesRoutingModule } from './finances-routing.module';
import { DashboardComponent } from './dashboard/dashboard.component';
import { FinancesComponent } from './finances.component';
import { SharedModule } from 'src/app/shared/shared.module';
import { MenuModule } from 'src/app/components/layout/menu';
import { MonthBarModule } from 'src/app/components/month-bar';
import { BankAccountSummaryDashboardModule } from 'src/app/components/bank-account-summary-dashboard';

@NgModule({
  declarations: [DashboardComponent, FinancesComponent],
  imports: [CommonModule, SharedModule, FinancesRoutingModule, MenuModule, MonthBarModule, BankAccountSummaryDashboardModule],
})
export class FinancesModule {}

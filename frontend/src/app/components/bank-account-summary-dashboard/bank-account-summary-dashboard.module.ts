import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BankAccountSummaryDashboardComponent } from './bank-account-summary-dashboard.component';
import { SharedModule } from 'src/app/shared';

@NgModule({
  declarations: [BankAccountSummaryDashboardComponent],
  imports: [CommonModule, SharedModule],
  exports: [BankAccountSummaryDashboardComponent],
})
export class BankAccountSummaryDashboardModule {}

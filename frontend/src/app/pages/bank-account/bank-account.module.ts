import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { BankAccountRoutingModule } from './bank-account-routing.module';
import { BankAccountComponent } from './bank-account.component';
import { SharedModule } from 'src/app/shared';
import { MatMenuModule } from '@angular/material/menu';
import { NewBankAccountComponent } from './new-bank-account/new-bank-account.component';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CovalentDialogsModule } from '@covalent/core/dialogs';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { MatCheckboxModule } from '@angular/material/checkbox';

import { BankAccountSingleComponent } from './bank-account-single/bank-account-single.component';
import { MonthBarModule } from 'src/app/components/month-bar';
import { TransactionsTableModule } from 'src/app/components/transactions-table/transactions-table.module';
import { BankAccountSummaryDashboardModule } from 'src/app/components/bank-account-summary-dashboard';

@NgModule({
  declarations: [BankAccountComponent, NewBankAccountComponent, BankAccountSingleComponent],
  imports: [
    CommonModule,
    BankAccountRoutingModule,
    MatMenuModule,
    ReactiveFormsModule,
    SharedModule,
    MatFormFieldModule,
    MatInputModule,
    CovalentDialogsModule,
    MonthBarModule,
    TransactionsTableModule,
    NgxChartsModule,
    BankAccountSummaryDashboardModule,
    MatCheckboxModule,
  ],
})
export class BankAccountModule {}

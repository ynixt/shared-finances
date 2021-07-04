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

import { BankAccountSingleComponent } from './bank-account-single/bank-account-single.component';
import { MonthBarModule } from 'src/app/components/month-bar';
import { TransactionsTableModule } from 'src/app/components/transactions-table/transactions-table.module';

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
  ],
})
export class BankAccountModule {}

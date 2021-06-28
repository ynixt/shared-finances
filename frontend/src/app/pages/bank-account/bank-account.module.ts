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
import { CovalentDataTableModule } from '@covalent/core/data-table';
import { CovalentPagingModule } from '@covalent/core/paging';

import { BankAccountSingleComponent } from './bank-account-single/bank-account-single.component';
import { MatOptionModule } from '@angular/material/core';
import { MonthBarModule } from 'src/app/components/month-bar';

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
    CovalentDataTableModule,
    CovalentPagingModule,
    MatOptionModule,
    MonthBarModule,
  ],
})
export class BankAccountModule {}

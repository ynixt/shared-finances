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

@NgModule({
  declarations: [BankAccountComponent, NewBankAccountComponent],
  imports: [
    CommonModule,
    BankAccountRoutingModule,
    MatMenuModule,
    ReactiveFormsModule,
    SharedModule,
    MatFormFieldModule,
    MatInputModule,
    CovalentDialogsModule,
  ],
})
export class BankAccountModule {}

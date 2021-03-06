import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { CovalentDialogsModule } from '@covalent/core/dialogs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { NewTransactionComponent } from './new-transaction.component';
import { SharedModule } from 'src/app/shared';
import { NewTransactionDialogService } from './new-transaction-dialog.service';
import { ReactiveFormsModule } from '@angular/forms';
import { CurrencyMaskModule } from 'ng2-currency-mask';
import { MatSelectModule } from '@angular/material/select';
import { BankAccountInputModule, CreditCardInputModule } from '../input';
import { CategoryInputModule } from '../input/category-input';

@NgModule({
  declarations: [NewTransactionComponent],
  imports: [
    CommonModule,
    SharedModule,
    CovalentDialogsModule,
    MatButtonToggleModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    CurrencyMaskModule,
    MatSelectModule,
    MatAutocompleteModule,
    CreditCardInputModule,
    BankAccountInputModule,
    MatSlideToggleModule,
    CategoryInputModule,
  ],
  providers: [NewTransactionDialogService],
})
export class NewTransactionModule {}

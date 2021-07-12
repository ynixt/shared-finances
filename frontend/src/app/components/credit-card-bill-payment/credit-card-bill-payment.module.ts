import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CovalentDialogsModule } from '@covalent/core/dialogs';
import { CurrencyMaskModule } from 'ng2-currency-mask';

import { CreditCardBillPaymentComponent } from './credit-card-bill-payment.component';
import { SharedModule } from 'src/app/shared';
import { CreditCardBillPaymentDialogService } from './credit-card-bill-payment-dialog.service';
import { BankAccountInputModule } from '../input';

@NgModule({
  declarations: [CreditCardBillPaymentComponent],
  providers: [CreditCardBillPaymentDialogService],
  imports: [
    CommonModule,
    SharedModule,
    CovalentDialogsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    CurrencyMaskModule,
    BankAccountInputModule,
  ],
  exports: [CreditCardBillPaymentComponent],
})
export class CreditCardBillPaymentModule {}

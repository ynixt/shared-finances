import { Component, EventEmitter, Inject, OnInit, Output } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import moment, { Moment } from 'moment';
import { take } from 'rxjs/operators';
import { TransactionType } from 'src/app/@core/enums';

import { CreditCard, Transaction } from 'src/app/@core/models';
import { ErrorService, TransactionService } from 'src/app/@core/services';
import { CreditCardBillPaymentComponentArgs } from './credit-card-bill-payment-component-args';

const initialValue = -0.01;

@Component({
  selector: 'app-credit-card-bill-payment',
  templateUrl: './credit-card-bill-payment.component.html',
  styleUrls: ['./credit-card-bill-payment.component.scss'],
})
export class CreditCardBillPaymentComponent implements OnInit {
  @Output() closed: EventEmitter<void> = new EventEmitter();

  formGroup: UntypedFormGroup;
  editingTransaction: Transaction;
  creditCard: CreditCard;
  creditCardBillDate: Moment;
  noBill: boolean;

  private billValue: number;

  constructor(
    @Inject(MAT_DIALOG_DATA) data: CreditCardBillPaymentComponentArgs,
    private transactionService: TransactionService,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
  ) {
    this.editingTransaction = data.transaction;
    this.creditCard = data.creditCard;
    this.creditCardBillDate = moment(data.creditCardBillDate);
    this.noBill = data.billValue == null;
    this.billValue = data.billValue ?? initialValue;
  }

  ngOnInit(): void {
    this.formGroup = new UntypedFormGroup({
      value: new UntypedFormControl(this.editingTransaction?.value ?? this.billValue, [Validators.required]),
      bankAccount: new UntypedFormControl(undefined, [Validators.required]),
      date: new UntypedFormControl(this.editingTransaction?.date ? moment(this.editingTransaction?.date) : moment().startOf('day'), [
        Validators.required,
      ]),
      creditCardBillDate: new UntypedFormControl(
        {
          value: this.editingTransaction?.creditCardBillDateValue
            ? moment(this.editingTransaction?.creditCardBillDateValue)
            : this.creditCardBillDate,
          disabled: true,
        },
        [Validators.required],
      ),
      description: new UntypedFormControl(this.editingTransaction?.description, [Validators.maxLength(50)]),
    });
  }

  async save(): Promise<void> {
    if (this.formGroup.valid) {
      await this.transactionService
        .payCreditCardBill({
          value: this.formGroup.value.value,
          bankAccountId: this.formGroup.value.bankAccount.accountId,
          creditCardId: this.creditCard.id,
          description: this.formGroup.value.description,
          creditCardBillDateValue: this.creditCardBillDate.toISOString(),
          date: this.formGroup.value.date,
        })
        .pipe(
          take(1),
          this.toast.observe({
            loading: this.translocoService.translate('paying'),
            success: this.translocoService.translate('credit-card-bill-paying-successful'),
            error: error =>
              this.errorService.getInstantErrorMessage(
                error,
                'credit-card-bill-paying-error-no-name',
                'credit-card-bill-paying-error-with-description',
              ),
          }),
        )
        .toPromise();

      this.closed.next();
    }
  }
}

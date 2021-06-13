import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import moment from 'moment';
import { take } from 'rxjs/operators';
import { TransactionType } from 'src/app/@core/enums';
import { BankAccount } from 'src/app/@core/models';
import { ErrorService } from 'src/app/@core/services/error.service';
import { AuthSelectors, BankAccountSelectors } from 'src/app/store/services/selectors';
import { NewTransactionService } from './new-transaction.service';

interface AccountWithPerson {
  person: string;
  accounts: BankAccount[];
}

@Component({
  selector: 'app-new-transaction',
  templateUrl: './new-transaction.component.html',
  styleUrls: ['./new-transaction.component.scss'],
})
export class NewTransactionComponent implements OnInit {
  @Output() closed: EventEmitter<void> = new EventEmitter();

  formGroup: FormGroup;
  transactionTypeEnum = TransactionType;

  accountsWithPersons: AccountWithPerson[] = [];

  constructor(
    private newTransactionService: NewTransactionService,
    private bankAccountSelectors: BankAccountSelectors,
    private authSelectors: AuthSelectors,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
  ) {}

  get transactionType() {
    return this.formGroup?.value?.transactionType;
  }

  get date() {
    return this.formGroup?.value?.date;
  }

  ngOnInit(): void {
    this.formGroup = new FormGroup({
      transactionType: new FormControl(TransactionType.Revenue, [Validators.required]),
      date: new FormControl(moment().startOf('day'), [Validators.required]),
      value: new FormControl('', [Validators.required]),
      description: new FormControl('', [Validators.maxLength(50)]),
      bankAccount: new FormControl('', [Validators.required]),
    });

    this.mountAccounts();
  }

  async newTransacation(): Promise<void> {
    if (this.formGroup.valid) {
      const transacationSaved = await this.newTransactionService
        .newTransaction({
          transactionType: this.formGroup.value.transactionType,

          date: this.formGroup.value.date,

          value: this.formGroup.value.value,

          description: this.formGroup.value.description,

          bankAccountId: this.formGroup.value.bankAccount,
        })
        .pipe(
          take(1),
          this.toast.observe({
            loading: this.translocoService.translate('creating'),
            success: this.translocoService.translate('transacation-creating-successful'),
            error: error =>
              this.errorService.getInstantErrorMessage(
                error,
                'transacation-creating-error-no-name',
                'transacation-creating-error-with-description',
              ),
          }),
        )
        .toPromise();
    }
  }

  private async mountAccounts() {
    this.accountsWithPersons = [];

    const user = await this.authSelectors.currentUser();

    this.accountsWithPersons.push({ person: user.name, accounts: await this.bankAccountSelectors.currentBankAccounts() });
  }
}

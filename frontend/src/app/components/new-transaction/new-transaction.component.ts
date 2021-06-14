import { Component, EventEmitter, OnInit, Output, AfterContentChecked, ChangeDetectorRef } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, Validators } from '@angular/forms';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import moment from 'moment';
import { combineLatest, Observable } from 'rxjs';
import { map, startWith, take } from 'rxjs/operators';
import { TransactionType } from 'src/app/@core/enums';
import { BankAccount, Category, CreditCard } from 'src/app/@core/models';
import { ErrorService } from 'src/app/@core/services/error.service';
import { AuthSelectors, BankAccountSelectors, CreditCardSelectors, UserCategorySelectors } from 'src/app/store/services/selectors';
import { NewTransactionService } from './new-transaction.service';

interface AccountWithPerson {
  person: string;
  accounts: BankAccount[];
}

interface CreditCardWithPerson {
  person: string;
  creditCards: CreditCard[];
}

function requiredWhenTransactionTypeIsNotCredit(formControl: AbstractControl) {
  if (!formControl.parent) {
    return null;
  }

  if (formControl.parent.get('transactionType').value !== TransactionType.CreditCard) {
    return Validators.required(formControl);
  }

  return null;
}

function requiredWhenTransactionTypeIsTransfer(formControl: AbstractControl) {
  if (!formControl.parent) {
    return null;
  }

  if (formControl.parent.get('transactionType').value === TransactionType.Transfer) {
    return Validators.required(formControl);
  }

  return null;
}

function requiredWhenTransactionTypeIsCredit(formControl: AbstractControl) {
  if (!formControl.parent) {
    return null;
  }

  if (formControl.parent.get('transactionType').value === TransactionType.CreditCard) {
    return Validators.required(formControl);
  }

  return null;
}

const initialValue = 0.01;

@UntilDestroy()
@Component({
  selector: 'app-new-transaction',
  templateUrl: './new-transaction.component.html',
  styleUrls: ['./new-transaction.component.scss'],
})
export class NewTransactionComponent implements OnInit, AfterContentChecked {
  @Output() closed: EventEmitter<void> = new EventEmitter();

  formGroup: FormGroup;
  transactionTypeEnum = TransactionType;

  accountsWithPersons: AccountWithPerson[] = [];

  creditCardsWithPersons$: Observable<CreditCardWithPerson[]>;
  filteredCategories$: Observable<Category[]>;

  constructor(
    private newTransactionService: NewTransactionService,
    private bankAccountSelectors: BankAccountSelectors,
    private authSelectors: AuthSelectors,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
    private userCategorySelectors: UserCategorySelectors,
    private creditCardSelectors: CreditCardSelectors,
    private cdRef: ChangeDetectorRef,
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
      value: new FormControl(initialValue, [Validators.required]),
      description: new FormControl(undefined, [Validators.maxLength(50)]),
      bankAccount: new FormControl(undefined, requiredWhenTransactionTypeIsNotCredit),
      bankAccount2: new FormControl(undefined, requiredWhenTransactionTypeIsTransfer),
      category: new FormControl(undefined),
      creditCard: new FormControl(undefined, requiredWhenTransactionTypeIsCredit),
    });

    this.mountAccounts();
    this.mountFilteredCategories();
    this.mountCreditCards();

    this.formGroup
      .get('transactionType')
      .valueChanges.pipe(untilDestroyed(this))
      .subscribe(transactionType => {
        const value = this.formGroup.value.value || initialValue;

        this.formGroup.get('value').setValue(this.ifNecessaryMakeValueNegative(value, transactionType));

        if (transactionType !== TransactionType.Transfer) {
          this.formGroup.get('bankAccount2').setValue(undefined);
        }

        if (transactionType === TransactionType.CreditCard) {
          this.formGroup.get('bankAccount').setValue(undefined);
        } else {
          this.formGroup.get('creditCard').setValue(undefined);
        }

        this.formGroup.get('bankAccount').updateValueAndValidity();
        this.formGroup.get('creditCard').updateValueAndValidity();
      });
  }

  ngAfterContentChecked(): void {
    this.cdRef.detectChanges();
  }

  async newTransacation(): Promise<void> {
    if (this.formGroup.valid) {
      await this.newTransactionService
        .newTransaction({
          transactionType: this.formGroup.value.transactionType,
          date: this.formGroup.value.date,
          value: this.ifNecessaryMakeValueNegative(this.formGroup.value.value, this.formGroup.value.transactionType),
          description: this.formGroup.value.description,
          bankAccountId: this.formGroup.value.bankAccount,
          bankAccount2Id: this.formGroup.value.bankAccount2,
          creditCardId: this.formGroup.value.creditCard,
          categoryId: this.formGroup.value.category?.id,
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

  formatCategory(category: Category): string {
    return category?.name;
  }

  private async mountAccounts() {
    this.accountsWithPersons = [];

    const user = await this.authSelectors.currentUser();

    this.accountsWithPersons.push({ person: user.name, accounts: await this.bankAccountSelectors.currentBankAccounts() });
  }

  private mountFilteredCategories(): void {
    this.filteredCategories$ = combineLatest([
      this.formGroup.get('category').valueChanges.pipe(startWith('')),
      this.userCategorySelectors.categories$,
    ]).pipe(
      map(combined => ({ categories: combined[1], query: combined[0] })),
      map(combined => {
        return combined.categories != null
          ? combined.categories
              .filter(category => {
                const query = typeof combined.query === 'string' ? combined.query : combined.query.name;

                return category.name.toLowerCase().includes(query.toLowerCase());
              })
              .sort((categoryA, categoryB) => categoryA.name.localeCompare(categoryB.name))
          : [];
      }),
    );
  }

  private mountCreditCards(): void {
    this.creditCardsWithPersons$ = combineLatest([this.creditCardSelectors.creditCards$, this.authSelectors.user$]).pipe(
      map(combined => ({ creditCards: combined[0], user: combined[1] })),
      map(combined => [
        {
          person: combined.user.name,
          creditCards: combined.creditCards.sort((creditCardA, creditCardB) => creditCardA.name.localeCompare(creditCardB.name)),
        },
      ]),
    );
  }

  private isTransactionNegative(transactionType: TransactionType): boolean {
    return [TransactionType.CreditCard, TransactionType.Expense].includes(transactionType);
  }

  private ifNecessaryMakeValueNegative(value: number, transactionType: TransactionType): number {
    const isTransactionNegative = this.isTransactionNegative(transactionType);

    if ((isTransactionNegative && value > 0) || (!isTransactionNegative && value < 0)) {
      return value * -1;
    }

    return value;
  }
}

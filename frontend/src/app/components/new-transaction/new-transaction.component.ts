import { Component, EventEmitter, OnInit, Output, AfterContentChecked, ChangeDetectorRef, OnDestroy, Inject } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import moment, { Moment } from 'moment';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { map, startWith, take } from 'rxjs/operators';
import { TransactionType } from 'src/app/@core/enums';
import { BankAccount, Category, CreditCard, Transaction, User } from 'src/app/@core/models';
import { Group } from 'src/app/@core/models/group';
import { GroupsService, TitleService, TransactionService } from 'src/app/@core/services';
import { ErrorService } from 'src/app/@core/services/error.service';
import { AuthSelectors, BankAccountSelectors, CreditCardSelectors, UserCategorySelectors } from 'src/app/store/services/selectors';
import { NewTransactionComponentArgs } from './new-transaction-component-args';

interface AccountWithPerson {
  person: Partial<User>;
  accounts: BankAccount[];
}

interface CreditCardWithPerson {
  person: Partial<User>;
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

function requiredIfShared(shared: boolean, formControl: AbstractControl) {
  if (!formControl.parent) {
    return null;
  }

  if (shared) {
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
export class NewTransactionComponent implements OnInit, AfterContentChecked, OnDestroy {
  @Output() closed: EventEmitter<void> = new EventEmitter();

  formGroup: FormGroup;
  transactionTypeEnum = TransactionType;
  accountsWithPersons: AccountWithPerson[] = [];
  creditCardsWithPersons$: Observable<CreditCardWithPerson[]>;
  filteredCategories$: Observable<Category[]>;
  shared: boolean;
  groups: Group[];
  editingTransaction: Transaction;
  creditCardBillDateOptions: Moment[] = [];

  private previousTitle: string;
  private creditCardFromOtherUsers = new BehaviorSubject<CreditCardWithPerson[]>([]);

  constructor(
    private transactionService: TransactionService,
    private bankAccountSelectors: BankAccountSelectors,
    private authSelectors: AuthSelectors,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
    private userCategorySelectors: UserCategorySelectors,
    private creditCardSelectors: CreditCardSelectors,
    private cdRef: ChangeDetectorRef,
    private titleService: TitleService,
    private groupsService: GroupsService,
    @Inject(MAT_DIALOG_DATA) data: NewTransactionComponentArgs,
  ) {
    this.shared = data.shared;
    this.editingTransaction = data.transaction != null ? { ...data.transaction } : undefined;
  }

  get transactionType() {
    return this.formGroup?.value?.transactionType;
  }

  get date() {
    return this.formGroup?.value?.date;
  }

  get creditCard() {
    return this.formGroup?.value?.creditCard;
  }

  get group() {
    return this.formGroup?.value?.group;
  }

  get creditCardBillDateFormControl() {
    return this.formGroup.get('creditCardBillDate');
  }

  get creditCardBillDate() {
    return this.creditCardBillDateFormControl.value;
  }

  async ngOnInit(): Promise<void> {
    this.createFormGroup();

    this.previousTitle = await this.titleService.getCurrentTitle();
    this.titleService.changeTitle('new-transaction');

    if (this.date != null && this.creditCard != null) {
      this.creditCardBillDateFormControl.enable();
    }

    this.mountGroups();
    this.mountAccounts();
    this.mountFilteredCategories();
    this.mountCreditCards();

    if (this.shared) {
      this.formGroup
        .get('group')
        .valueChanges.pipe(untilDestroyed(this))
        .subscribe(group => {
          this.mountAccounts(group);
        });
    }

    this.watchTransactionTypeChanges();
    this.mountCreditCardBillDateOptions();
  }

  async ngOnDestroy(): Promise<void> {
    await this.titleService.changeTitle(this.previousTitle);
  }

  ngAfterContentChecked(): void {
    this.cdRef.detectChanges();
  }

  async saveTransacation(): Promise<void> {
    if (this.formGroup.valid) {
      let user: Partial<User>;
      let user2: Partial<User>;

      const bankAccount = this.formGroup.value.bankAccount;
      const bankAccount2 = this.formGroup.value.bankAccount2;
      const creditCard = this.formGroup.value.creditCard;

      if (bankAccount) {
        user = { id: bankAccount.personId };

        if (bankAccount2) {
          user2 = { id: bankAccount2.personId };
        }
      } else {
        user = { id: creditCard.personId };
      }

      await (this.editingTransaction
        ? this.editTransacation(bankAccount, bankAccount2, creditCard, user, user2)
        : this.newTransacation(bankAccount, bankAccount2, creditCard, user, user2));
      this.closed.next();
    }
  }

  formatCategory(category: Category): string {
    return category?.name;
  }

  creditCardInputValueCompare(obj1: any, obj2: any) {
    return obj1 === obj2 || (obj1 && obj2 && obj1.creditCardId === obj2.creditCardId && obj1.creditsPerson === obj2.creditsPerson);
  }

  bankAccountInputValueCompare(obj1: any, obj2: any) {
    return obj1 === obj2 || (obj1 && obj2 && obj1.accountId === obj2.accountId && obj1.personId === obj2.personId);
  }

  private async editTransacation(
    bankAccount: any,
    bankAccount2: any,
    creditCard: any,
    user: Partial<User>,
    user2: Partial<User>,
  ): Promise<void> {
    await this.transactionService
      .editTransaction({
        id: this.editingTransaction.id,
        transactionType: this.formGroup.value.transactionType,
        date: this.formGroup.value.date,
        value: this.ifNecessaryMakeValueNegative(this.formGroup.value.value, this.formGroup.value.transactionType),
        description: this.formGroup.value.description,
        bankAccountId: bankAccount?.accountId,
        bankAccount2Id: bankAccount2?.accountId,
        creditCardId: creditCard?.creditCardId,
        categoryId: this.formGroup.value.category?.id,
        groupId: this.group?.id,
        user,
        user2,
        creditCardBillDate: this.creditCardBillDate,
      })
      .pipe(
        take(1),
        this.toast.observe({
          loading: this.translocoService.translate('editing'),
          success: this.translocoService.translate('transacation-editing-successful'),
          error: error =>
            this.errorService.getInstantErrorMessage(
              error,
              'transacation-editing-error-no-name',
              'transacation-editing-error-with-description',
            ),
        }),
      )
      .toPromise();
  }

  private async newTransacation(
    bankAccount: any,
    bankAccount2: any,
    creditCard: any,
    user: Partial<User>,
    user2: Partial<User>,
  ): Promise<void> {
    await this.transactionService
      .newTransaction({
        transactionType: this.formGroup.value.transactionType,
        date: this.formGroup.value.date,
        value: this.ifNecessaryMakeValueNegative(this.formGroup.value.value, this.formGroup.value.transactionType),
        description: this.formGroup.value.description,
        bankAccountId: bankAccount?.accountId,
        bankAccount2Id: bankAccount2?.accountId,
        creditCardId: creditCard?.creditCardId,
        categoryId: this.formGroup.value.category?.id,
        groupId: this.group?.id,
        user,
        user2,
        creditCardBillDate: this.creditCardBillDate,
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

  private async mountAccounts(group?: Group) {
    this.accountsWithPersons = [];

    const user = await this.authSelectors.currentUser();

    this.accountsWithPersons.push({ person: user, accounts: await this.bankAccountSelectors.currentBankAccounts() });

    const creditCardFromOtherUsers: CreditCardWithPerson[] = [];

    if (this.shared) {
      group?.users.forEach(userFromGroup => {
        if (userFromGroup.id !== user.id) {
          if (userFromGroup.bankAccounts?.length > 0) {
            this.accountsWithPersons.push({ person: userFromGroup, accounts: userFromGroup.bankAccounts });
          }

          if (userFromGroup.creditCards?.length > 0) {
            creditCardFromOtherUsers.push({ person: userFromGroup, creditCards: userFromGroup.creditCards });
          }
        }
      });
    }

    this.creditCardFromOtherUsers.next(creditCardFromOtherUsers);

    this.accountsWithPersons = this.accountsWithPersons.sort((a, b) => a.person.name.localeCompare(b.person.name));

    if (this.editingTransaction?.bankAccountId != null) {
      const accountWithPerson = this.accountsWithPersons.find(
        accountWithPerson =>
          accountWithPerson.accounts.find(bankAccount => bankAccount.id === this.editingTransaction.bankAccountId) != null,
      );

      this.formGroup
        .get('bankAccount')
        .setValue({ accountId: this.editingTransaction.bankAccountId, personId: accountWithPerson.person.id });
    }
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
    this.creditCardsWithPersons$ = combineLatest([
      this.creditCardSelectors.creditCards$,
      this.authSelectors.user$,
      this.creditCardFromOtherUsers.asObservable(),
    ]).pipe(
      map(combined => ({ creditCards: combined[0], user: combined[1], creditCardFromOtherUsers: combined[2] })),
      map(combined =>
        [
          {
            person: combined.user,
            creditCards: combined.creditCards.sort((creditCardA, creditCardB) => creditCardA.name.localeCompare(creditCardB.name)),
          },
          ...combined.creditCardFromOtherUsers,
        ].sort((a, b) => a.person.name.localeCompare(b.person.name)),
      ),
    );

    if (this.editingTransaction?.creditCardId != null) {
      this.creditCardsWithPersons$.pipe(take(1)).subscribe(creditCardsWithPersons => {
        const creditCardWithPerson = creditCardsWithPersons.find(
          creditCardsWithPerson =>
            creditCardsWithPerson.creditCards.find(creditCard => creditCard.id === this.editingTransaction.creditCardId) != null,
        );

        this.formGroup
          .get('creditCard')
          .setValue({ creditCardId: this.editingTransaction.creditCardId, personId: creditCardWithPerson.person.id });
      });
    }
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

  private async mountGroups(): Promise<void> {
    this.groups = this.shared ? await this.groupsService.getGroupsWithUsers() : undefined;

    if (this.editingTransaction?.group != null) {
      this.formGroup.get('group').setValue(this.groups.find(group => group.id === this.editingTransaction.group.id));
    }
  }

  private createFormGroup(): void {
    this.formGroup = new FormGroup({
      transactionType: new FormControl(this.editingTransaction?.transactionType ?? TransactionType.Revenue, [Validators.required]),
      date: new FormControl(this.editingTransaction?.date ? moment(this.editingTransaction?.date) : moment().startOf('day'), [
        Validators.required,
      ]),
      creditCardBillDate: new FormControl(
        {
          value: this.editingTransaction?.creditCardBillDate ? moment(this.editingTransaction?.creditCardBillDate) : '',
          disabled: true,
        },
        [requiredWhenTransactionTypeIsCredit],
      ),
      value: new FormControl(this.editingTransaction?.value ?? initialValue, [Validators.required]),
      description: new FormControl(this.editingTransaction?.description, [Validators.maxLength(50)]),
      bankAccount: new FormControl(undefined, requiredWhenTransactionTypeIsNotCredit),
      bankAccount2: new FormControl(undefined, requiredWhenTransactionTypeIsTransfer),
      category: new FormControl(this.editingTransaction?.category),
      creditCard: new FormControl(undefined, requiredWhenTransactionTypeIsCredit),
      group: new FormControl(undefined, formControl => requiredIfShared(this.shared, formControl)),
    });
  }

  private watchTransactionTypeChanges(): void {
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
          this.creditCardBillDateFormControl.setValue(undefined);
        }

        this.formGroup.get('bankAccount').updateValueAndValidity();
        this.formGroup.get('creditCard').updateValueAndValidity();
        this.creditCardBillDateFormControl.updateValueAndValidity();
      });
  }

  private mountCreditCardBillDateOptions(): void {
    combineLatest([
      this.formGroup.get('date').valueChanges.pipe(startWith(this.date)),
      this.formGroup.get('creditCard').valueChanges.pipe(startWith(this.creditCard)),
      this.creditCardsWithPersons$,
    ])
      .pipe(untilDestroyed(this))
      .subscribe(combined => {
        const newOptions: Moment[] = [];

        const newDate: string = combined[0];
        const newCreditCardWithPerson: { creditCardId: string; personId: string } = combined[1];
        const creditCardsWithPersons: CreditCardWithPerson[] = combined[2];

        let closingDay: number;

        if (newDate != null && newCreditCardWithPerson != null && creditCardsWithPersons != null) {
          const newCreditCard = creditCardsWithPersons
            .find(obj => obj.person.id === newCreditCardWithPerson.personId)
            .creditCards.find(creditCard => creditCard.id === newCreditCardWithPerson.creditCardId);

          closingDay = newCreditCard.closingDay;

          for (let i = -2; i <= 2; i++) {
            const dateForThisOption = moment(newDate).date(closingDay);

            if (dateForThisOption.isSame(newDate, 'month') === false) {
              // month with less days, like february

              dateForThisOption.subtract(1, 'day');
            }

            newOptions.push(dateForThisOption.add(i, 'month'));
          }
          this.creditCardBillDateFormControl.enable();
        } else {
          this.creditCardBillDateFormControl.disable();
        }

        this.creditCardBillDateOptions = newOptions;

        this.setCreditCardBillDate(newDate, closingDay);
      });
  }

  private setCreditCardBillDate(date: string, closingDay?: number) {
    let oneMonthSkipped = false;
    const billDate = this.creditCardBillDateOptions.find(option => {
      const isSameMonth = moment(date).isSame(option, 'month');
      const isSameOrAfterClosingDay = moment(date).date() >= closingDay;

      if (isSameMonth || oneMonthSkipped) {
        if (isSameOrAfterClosingDay && !oneMonthSkipped) {
          oneMonthSkipped = true;
          return false;
        } else {
          return true;
        }
      }

      return false;
    });

    this.creditCardBillDateFormControl.setValue(billDate);
    this.creditCardBillDateFormControl.updateValueAndValidity();
  }
}

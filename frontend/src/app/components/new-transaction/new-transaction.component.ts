import {
  Component,
  EventEmitter,
  OnInit,
  Output,
  AfterContentChecked,
  ChangeDetectorRef,
  OnDestroy,
  Inject,
  ViewChild,
} from '@angular/core';
import { AbstractControl, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import moment, { Moment } from 'moment';
import { combineLatest } from 'rxjs';
import { startWith, take } from 'rxjs/operators';
import { TransactionType } from 'src/app/@core/enums';
import { Transaction, User } from 'src/app/@core/models';
import { Group } from 'src/app/@core/models/group';
import { CreditCardService, GroupsService, TitleService, TransactionService } from 'src/app/@core/services';
import { ErrorService } from 'src/app/@core/services/error.service';
import { BankAccountInputComponent, CreditCardInputComponent, CreditCardWithPerson } from '../input';
import { NewTransactionComponentArgs } from './new-transaction-component-args';

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

  formGroup: UntypedFormGroup;
  transactionTypeEnum = TransactionType;

  shared: boolean;
  groups: Group[];
  editingTransaction: Transaction;
  creditCardBillDateOptions: Moment[] = [];
  loading = false;

  @ViewChild(CreditCardInputComponent) creditCardInput: CreditCardInputComponent;
  @ViewChild('bankAccount') bankAccountInput: BankAccountInputComponent;
  @ViewChild('bankAccountTwo') bankAccountTwoInput: BankAccountInputComponent;

  private previousTitle: string;

  constructor(
    private transactionService: TransactionService,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
    private cdRef: ChangeDetectorRef,
    private titleService: TitleService,
    private groupsService: GroupsService,
    @Inject(MAT_DIALOG_DATA) data: NewTransactionComponentArgs,
    private creditCardService: CreditCardService,
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

  get useInstallmentFormControl() {
    return this.formGroup.get('useInstallment');
  }

  get useInstallment() {
    return this.useInstallmentFormControl.value;
  }

  async ngOnInit(): Promise<void> {
    this.loading = true;

    await this.initializeComponent();

    this.loading = false;
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

  creditCardBillDateInputValueCompare(obj1: any, obj2: any) {
    return (
      obj1 === obj2 ||
      (obj1 != null && obj2 != null && 'toISOString' in obj1 && 'toISOString' in obj2 && obj1?.toISOString() === obj2?.toISOString())
    );
  }

  private async initializeComponent() {
    this.createFormGroup();

    this.previousTitle = await this.titleService.getCurrentTitle();
    this.titleService.changeTitle('new-transaction');

    if (this.transactionType === TransactionType.CreditCard) {
      this.mountCreditCardBillDateOptions();
    }

    if (this.shared) {
      await this.initializeComponentGroupTransaction();
    } else {
      await this.initializeComponentSingleTransaction();
    }

    this.watchTransactionTypeChanges();
  }

  private async initializeComponentSingleTransaction(): Promise<void> {
    await this.selectCurrentBankAccount();
    this.selectCurrentCreditCard();
  }

  private async initializeComponentGroupTransaction(): Promise<void> {
    await Promise.all([
      new Promise<void>(resolve => {
        if (this.editingTransaction == null) {
          // in a new transaction there's no group. So it's necessary resolve promise.
          resolve();
        }

        this.formGroup
          .get('group')
          .valueChanges.pipe(untilDestroyed(this))
          .subscribe(group => {
            Promise.all([
              this.bankAccountInput?.mountAccounts(group).then(() => this.selectCurrentBankAccount()),
              this.bankAccountTwoInput?.mountAccounts(group).then(() => this.selectCurrentBankAccountTwo()),
              this.creditCardInput?.mountCreditCards(group).then(() => this.selectCurrentCreditCard()),
            ])
              .then(() => resolve())
              .catch(err => console.error(err));
          });
      }),
      this.mountGroups(),
    ]);
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
        type: this.formGroup.value.transactionType,
        date: this.formGroup.value.date,
        value: this.transactionService.ifNecessaryMakeValueNegative(this.formGroup.value.value, this.formGroup.value.transactionType),
        description: this.formGroup.value.description,
        bankAccountId: bankAccount?.accountId,
        bankAccount2Id: bankAccount2?.accountId,
        creditCardId: creditCard?.creditCardId,
        categoryId: this.formGroup.value.category?.id,
        groupId: this.group?.id,
        user,
        user2,
        creditCardBillDateValue: this.creditCardBillDate,
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
        type: this.formGroup.value.transactionType,
        date: moment(this.formGroup.value.date).toISOString(),
        value: this.transactionService.ifNecessaryMakeValueNegative(this.formGroup.value.value, this.formGroup.value.transactionType),
        description: this.formGroup.value.description,
        bankAccountId: bankAccount?.accountId,
        bankAccount2Id: bankAccount2?.accountId,
        creditCardId: creditCard?.creditCardId,
        categoryId: this.formGroup.value.category?.id,
        groupId: this.group?.id,
        user,
        user2,
        creditCardBillDateValue: this.creditCardBillDate,
        totalInstallments: this.formGroup.value.totalInstallments,
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

  private async selectCurrentBankAccount() {
    if (this.editingTransaction?.bankAccountId != null) {
      this.bankAccountInput.selectBankAccount(this.editingTransaction.bankAccountId);
    }
  }
  private async selectCurrentBankAccountTwo() {
    if (this.editingTransaction?.bankAccount2Id != null) {
      this.bankAccountTwoInput.selectBankAccount(this.editingTransaction.bankAccount2Id);
    }
  }

  private selectCurrentCreditCard(): void {
    if (this.editingTransaction?.creditCardId != null) {
      this.creditCardInput.selectCreditCard(this.editingTransaction.creditCardId);
    }
  }

  private async mountGroups(): Promise<void> {
    this.groups = this.shared ? await this.groupsService.getGroupsWithUsers() : undefined;

    if (this.editingTransaction?.group != null) {
      this.formGroup.get('group').setValue(this.groups.find(group => group.id === this.editingTransaction.group.id));
    }
  }

  private createFormGroup(): void {
    this.formGroup = new UntypedFormGroup({
      transactionType: new UntypedFormControl(
        this.editingTransaction?.type ?? (this.shared ? TransactionType.Expense : TransactionType.Revenue),
        [Validators.required],
      ),
      date: new UntypedFormControl(this.editingTransaction?.date ? moment(this.editingTransaction?.date) : moment().startOf('day'), [
        Validators.required,
      ]),
      creditCardBillDate: new UntypedFormControl(
        {
          value: this.editingTransaction?.creditCardBillDateValue ? moment(this.editingTransaction?.creditCardBillDateValue) : '',
          disabled: true,
        },
        [requiredWhenTransactionTypeIsCredit],
      ),
      value: new UntypedFormControl(this.editingTransaction?.value ?? initialValue, [Validators.required]),
      description: new UntypedFormControl(this.editingTransaction?.description, [Validators.maxLength(50)]),
      bankAccount: new UntypedFormControl(undefined, requiredWhenTransactionTypeIsNotCredit),
      bankAccount2: new UntypedFormControl(undefined, requiredWhenTransactionTypeIsTransfer),
      category: new UntypedFormControl(this.editingTransaction?.category),
      creditCard: new UntypedFormControl(undefined, requiredWhenTransactionTypeIsCredit),
      group: new UntypedFormControl(undefined, formControl => requiredIfShared(this.shared, formControl)),
      useInstallment: new UntypedFormControl(this.editingTransaction?.installment != null),
      totalInstallments: new UntypedFormControl(this.editingTransaction?.installment, [Validators.min(2), Validators.max(200)]),
    });
  }

  private watchTransactionTypeChanges(): void {
    this.formGroup
      .get('transactionType')
      .valueChanges.pipe(untilDestroyed(this))
      .subscribe(transactionType => {
        const value = this.formGroup.value.value || initialValue;

        this.formGroup.get('group').setValue(undefined);
        this.formGroup.get('value').setValue(this.transactionService.ifNecessaryMakeValueNegative(value, transactionType));

        if (transactionType !== TransactionType.Transfer) {
          this.formGroup.get('bankAccount2').setValue(undefined);
        }

        if (transactionType === TransactionType.CreditCard) {
          this.formGroup.get('bankAccount').setValue(undefined);
          setTimeout(() => {
            this.mountCreditCardBillDateOptions();
          });
        } else {
          this.formGroup.get('creditCard').setValue(undefined);
          this.creditCardBillDateFormControl.setValue(undefined);
        }

        this.formGroup.get('group').updateValueAndValidity();
        this.formGroup.get('bankAccount').updateValueAndValidity();
        this.formGroup.get('creditCard').updateValueAndValidity();
        this.creditCardBillDateFormControl.updateValueAndValidity();
      });
  }

  private mountCreditCardBillDateOptions(): void {
    combineLatest([
      this.formGroup.get('date').valueChanges.pipe(startWith(this.date)),
      this.formGroup.get('creditCard').valueChanges.pipe(startWith(this.creditCard)),
      this.creditCardInput.creditCardsWithPersons$,
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
            newOptions.push(this.creditCardService.nextBillDate(newDate, closingDay, i));
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
    const billDate = this.creditCardService.findCreditCardBillDate(date, this.creditCardBillDateOptions, closingDay);

    this.creditCardBillDateFormControl.setValue(billDate);
    this.creditCardBillDateFormControl.updateValueAndValidity();
  }
}

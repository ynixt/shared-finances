import { Component, EventEmitter, OnInit, Output, AfterContentChecked, ChangeDetectorRef, OnDestroy, Inject } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import moment from 'moment';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { map, startWith, take } from 'rxjs/operators';
import { TransactionType } from 'src/app/@core/enums';
import { BankAccount, Category, CreditCard, User } from 'src/app/@core/models';
import { Group } from 'src/app/@core/models/group';
import { GroupsService, TitleService } from 'src/app/@core/services';
import { ErrorService } from 'src/app/@core/services/error.service';
import { AuthSelectors, BankAccountSelectors, CreditCardSelectors, UserCategorySelectors } from 'src/app/store/services/selectors';
import { NewTransactionComponentArgs } from './new-transaction-component-args';
import { NewTransactionService } from './new-transaction.service';

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

  private previousTitle: string;
  private creditCardFromOtherUsers = new BehaviorSubject<CreditCardWithPerson[]>([]);

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
    private titleService: TitleService,
    private groupsService: GroupsService,
    @Inject(MAT_DIALOG_DATA) data: NewTransactionComponentArgs,
  ) {
    this.shared = data.shared;
  }

  get transactionType() {
    return this.formGroup?.value?.transactionType;
  }

  get date() {
    return this.formGroup?.value?.date;
  }

  get group() {
    return this.formGroup?.value?.group;
  }

  async ngOnInit(): Promise<void> {
    this.formGroup = new FormGroup({
      transactionType: new FormControl(TransactionType.Revenue, [Validators.required]),
      date: new FormControl(moment().startOf('day'), [Validators.required]),
      value: new FormControl(initialValue, [Validators.required]),
      description: new FormControl(undefined, [Validators.maxLength(50)]),
      bankAccount: new FormControl(undefined, requiredWhenTransactionTypeIsNotCredit),
      bankAccount2: new FormControl(undefined, requiredWhenTransactionTypeIsTransfer),
      category: new FormControl(undefined),
      creditCard: new FormControl(undefined, requiredWhenTransactionTypeIsCredit),
      group: new FormControl(undefined, formControl => requiredIfShared(this.shared, formControl)),
    });

    this.previousTitle = await this.titleService.getCurrentTitle();
    this.titleService.changeTitle('new-transaction');

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

  async ngOnDestroy(): Promise<void> {
    await this.titleService.changeTitle(this.previousTitle);
  }

  ngAfterContentChecked(): void {
    this.cdRef.detectChanges();
  }

  async newTransacation(): Promise<void> {
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

      await this.newTransactionService
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

      this.closed.next();
    }
  }

  formatCategory(category: Category): string {
    return category?.name;
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
  }
}

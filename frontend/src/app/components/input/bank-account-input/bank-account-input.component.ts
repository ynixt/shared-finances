import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlContainer, ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ControlValueAccessorConnector } from 'src/app/@core/control-value-accessor-connector';
import { BankAccount, User } from 'src/app/@core/models';
import { Group } from 'src/app/@core/models/group';

import { AuthSelectors, BankAccountSelectors } from 'src/app/store/services/selectors';

export interface AccountWithPerson {
  person: Partial<User>;
  accounts: BankAccount[];
}

@Component({
  selector: 'app-bank-account-input',
  templateUrl: './bank-account-input.component.html',
  styleUrls: ['./bank-account-input.component.scss'],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => BankAccountInputComponent), multi: true }],
})
export class BankAccountInputComponent extends ControlValueAccessorConnector<AccountWithPerson> implements OnInit, ControlValueAccessor {
  accountsWithPersons: AccountWithPerson[] = [];

  @Input() title: string;
  @Input() autoMount = true;

  constructor(
    private authSelectors: AuthSelectors,
    private bankAccountSelectors: BankAccountSelectors,
    controlContainer: ControlContainer,
  ) {
    super(controlContainer);
  }

  ngOnInit(): void {
    if (this.autoMount) {
      this.mountAccounts();
    }
  }

  bankAccountInputValueCompare(obj1: any, obj2: any): boolean {
    return obj1 === obj2 || (obj1 && obj2 && obj1.accountId === obj2.accountId && obj1.personId === obj2.personId);
  }

  selectBankAccount(bankAccountId: string): void {
    const bankAccountWithPerson = this.accountsWithPersons.find(
      bankAccountsWithPerson => bankAccountsWithPerson.accounts.find(account => account.id === bankAccountId) != null,
    );

    if (bankAccountWithPerson) {
      this.control.setValue({ accountId: bankAccountId, personId: bankAccountWithPerson.person.id });
    }
  }

  async mountAccounts(group?: Group): Promise<void> {
    this.accountsWithPersons = [];

    const user = await this.authSelectors.currentUser();

    this.accountsWithPersons.push({
      person: user,
      accounts: (await this.bankAccountSelectors.currentBankAccounts()).filter(bankAccount =>
        this.shouldShowBankAccount(bankAccount, group != null),
      ),
    });

    if (group != null) {
      group?.users.forEach(userFromGroup => {
        if (userFromGroup.id !== user.id) {
          if (userFromGroup.bankAccounts?.length > 0) {
            this.accountsWithPersons.push({
              person: userFromGroup,
              accounts: userFromGroup.bankAccounts.filter(bankAccount => this.shouldShowBankAccount(bankAccount, true)),
            });
          }
        }
      });
    }

    this.accountsWithPersons = this.accountsWithPersons.sort((a, b) => a.person.name.localeCompare(b.person.name));
  }

  private shouldShowBankAccount(bankAccount: BankAccount, isShared: boolean): boolean {
    return bankAccount.enabled && (!isShared || (isShared && bankAccount.displayOnGroup));
  }
}

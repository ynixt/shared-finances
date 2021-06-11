import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import moment from 'moment';
import { TransactionType } from 'src/app/@core/enums';
import { BankAccount } from 'src/app/@core/models';
import { AuthSelectors, BankAccountSelectors } from 'src/app/store/services/selectors';

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

  constructor(private bankAccountSelectors: BankAccountSelectors, private authSelectors: AuthSelectors) {}

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

  private async mountAccounts() {
    this.accountsWithPersons = [];

    const user = await this.authSelectors.currentUser();

    this.accountsWithPersons.push({ person: user.name, accounts: await this.bankAccountSelectors.currentBankAccounts() });
  }
}

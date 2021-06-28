import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ITdDataTableColumn } from '@covalent/core/data-table';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import moment, { Moment } from 'moment';
import { switchMap } from 'rxjs/operators';
import { TransactionType } from 'src/app/@core/enums';
import { BankAccount, Page, Transaction } from 'src/app/@core/models';
import { BankAccountService } from '../bank-account.service';

@UntilDestroy()
@Component({
  selector: 'app-bank-account-single',
  templateUrl: './bank-account-single.component.html',
  styleUrls: ['./bank-account-single.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class BankAccountSingleComponent implements OnInit {
  bankAccount: BankAccount;
  pageSize = 20;

  transactionsPage$: Promise<Page<Transaction>>;
  balance: number;

  columns: ITdDataTableColumn[];

  private monthDate: Moment;

  constructor(private activatedRoute: ActivatedRoute, private bankAccountService: BankAccountService, private router: Router) {}

  ngOnInit(): void {
    this.activatedRoute.params
      .pipe(
        untilDestroyed(this),
        switchMap(params => {
          return this.bankAccountService.getBankAccount(params.bankAccountId);
        }),
      )
      .subscribe(bankAccount => this.getBankAccount(bankAccount));
  }

  public async getTransactions(page = 1): Promise<void> {
    this.transactionsPage$ = this.bankAccountService.getTransactions(
      this.bankAccount.id,
      { maxDate: moment(this.monthDate).endOf('month'), minDate: moment(this.monthDate).startOf('month') },
      { page, pageSize: this.pageSize },
    );
  }

  public async getBalance(): Promise<void> {
    this.balance = undefined;
    this.balance = await this.bankAccountService.getBalance(this.bankAccount.id, { maxDate: this.monthDate });
  }

  public async editTransaction(transaction: Transaction) {}

  public async deleteTransaction(transaction: Transaction) {}

  public getIconForTransaction(transaction: Transaction) {
    switch (transaction.transactionType) {
      case TransactionType.CreditCard:
        return 'credit_card';
      case TransactionType.Expense:
        return 'trending_down';
      case TransactionType.Revenue:
        return 'trending_up';
      case TransactionType.Transfer:
        return 'sync_alt';
    }
  }

  public getTransactionTranslateKey(transaction: Transaction) {
    switch (transaction.transactionType) {
      case TransactionType.CreditCard:
        return 'credit-card';
      case TransactionType.Expense:
        return 'expense';
      case TransactionType.Revenue:
        return 'revenue';
      case TransactionType.Transfer:
        return 'transfer';
    }
  }

  public getTransactionCssColor(transaction: Transaction) {
    switch (transaction.transactionType) {
      case TransactionType.CreditCard:
        return 'credit-card-color';
      case TransactionType.Expense:
        return 'expense-color';
      case TransactionType.Revenue:
        return 'revenue-color';
      case TransactionType.Transfer:
        return 'transfer-color';
    }
  }

  public async dateChanged(newDate: Moment): Promise<void> {
    this.monthDate = newDate;
    await this.getInfoBasedOnBank();
  }

  private async getBankAccount(bankAccount: BankAccount) {
    if (bankAccount == null) {
      this.router.navigateByUrl('/404');
      return;
    }

    this.bankAccount = bankAccount;
  }

  private getInfoBasedOnBank() {
    return Promise.all([this.getBalance(), this.getTransactions()]);
  }
}

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { TransactionType } from 'src/app/@core/enums';
import { Page, Transaction } from 'src/app/@core/models';

export interface TransactionsRequested {
  page: number;
}

@Component({
  selector: 'app-transactions-table',
  templateUrl: './transactions-table.component.html',
  styleUrls: ['./transactions-table.component.scss'],
})
export class TransactionsTableComponent implements OnInit {
  @Input() transactionsPage$: Promise<Page<Transaction>>;
  @Input() pageSize = 20;

  @Output() getTransactionsRequested = new EventEmitter<TransactionsRequested>();
  @Output() editTransactionRequested = new EventEmitter<Transaction>();
  @Output() deleteTransactionRequested = new EventEmitter<Transaction>();

  constructor() {}

  ngOnInit(): void {}

  public getTransactions(page = 1): void {
    this.getTransactionsRequested.next({ page });
  }

  public editTransaction(transaction: Transaction) {
    this.editTransactionRequested.next(transaction);
  }

  public deleteTransaction(transaction: Transaction) {
    this.deleteTransactionRequested.next(transaction);
  }

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
}

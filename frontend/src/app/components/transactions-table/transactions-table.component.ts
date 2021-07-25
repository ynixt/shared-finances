import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TdDialogService } from '@covalent/core/dialogs';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { Moment } from 'moment';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';
import { TransactionType } from 'src/app/@core/enums';
import { Page, Transaction } from 'src/app/@core/models';
import { ErrorService, TransactionService } from 'src/app/@core/services';
import { DateUtil } from 'src/app/@core/util';

export interface TransactionsRequested {
  page: number;
}

@Component({
  selector: 'app-transactions-table',
  templateUrl: './transactions-table.component.html',
  styleUrls: ['./transactions-table.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class TransactionsTableComponent implements OnInit {
  @Input() transactionsPage$: Promise<Page<Transaction>> | Observable<Page<Transaction>>;
  @Input() pageSize = 20;

  @Output() getTransactionsRequested = new EventEmitter<TransactionsRequested>();
  @Output() editTransactionRequested = new EventEmitter<Transaction>();
  @Output() deleteTransactionRequested = new EventEmitter<Transaction>();
  @Output() deleteTransactionAllInstallmentsRequested = new EventEmitter<Transaction>();
  @Output() deleteTransactionNextInstallmentsRequested = new EventEmitter<Transaction>();

  constructor(
    private dialogService: TdDialogService,
    private translocoService: TranslocoService,
    private transactionService: TransactionService,
    private toast: HotToastService,
    private errorService: ErrorService,
  ) {}

  ngOnInit(): void {}

  public getTransactions(page = 1): void {
    this.getTransactionsRequested.next({ page });
  }

  public editTransaction(transaction: Transaction): void {
    this.editTransactionRequested.next(transaction);
  }

  public dateIsBiggerThanToday(date: string | Moment): boolean {
    return DateUtil.dateIsBiggerThanToday(date);
  }

  public async deleteTransaction(transaction: Transaction): Promise<void> {
    const confirm = await this.showDeleteConfirmation(
      this.translocoService.translate('delete-confirm', { name: transaction.description ?? '' }),
    );

    if (confirm) {
      await this.transactionService
        .deleteTransaction(transaction.id)
        .pipe(
          take(1),
          this.toast.observe({
            loading: this.translocoService.translate('deleting'),
            success: this.translocoService.translate('deleting-successful', { name: transaction.description ?? '' }),
            error: error =>
              this.errorService.getInstantErrorMessage(error, 'deleting-error', 'deleting-error-with-description', {
                name: transaction.description,
              }),
          }),
        )
        .toPromise();

      this.deleteTransactionRequested.next(transaction);
    }
  }

  public async deleteTransactionAllInstallments(transaction: Transaction): Promise<void> {
    const confirm = await this.showDeleteConfirmation(
      this.translocoService.translate('delete-all-installments-confirm', {
        name: transaction.description ?? '',
        totalInstallments: transaction.totalInstallments,
      }),
    );

    if (confirm) {
      await this.transactionService
        .deleteTransaction(transaction.id, { deleteAllInstallments: true })
        .pipe(
          take(1),
          this.toast.observe({
            loading: this.translocoService.translate('deleting'),
            success: this.translocoService.translate('deleting-successful-all-installments', { name: transaction.description ?? '' }),
            error: error =>
              this.errorService.getInstantErrorMessage(
                error,
                'deleting-error-all-installments',
                'deleting-error-with-description-all-installments',
                {
                  name: transaction.description,
                },
              ),
          }),
        )
        .toPromise();

      this.deleteTransactionAllInstallmentsRequested.next(transaction);
    }
  }

  public async deleteTransactionNextInstallments(transaction: Transaction): Promise<void> {
    const installments = [];

    for (let i = transaction.installment; i <= transaction.totalInstallments; i++) {
      installments.push(i);
    }

    const installmentsByComma = installments.join(', ');

    const confirm = await this.showDeleteConfirmation(
      this.translocoService.translate('delete-next-installments-confirm', {
        name: transaction.description ?? '',
        installments: installmentsByComma,
      }),
    );

    if (confirm) {
      await this.transactionService
        .deleteTransaction(transaction.id, { deleteNextInstallments: true })
        .pipe(
          take(1),
          this.toast.observe({
            loading: this.translocoService.translate('deleting'),
            success: this.translocoService.translate('deleting-successful-next-installments', {
              name: transaction.description ?? '',
              installments: installmentsByComma,
            }),
            error: error =>
              this.errorService.getInstantErrorMessage(
                error,
                'deleting-error-next-installments',
                'deleting-error-with-description-next-installments',
                {
                  name: transaction.description,
                  installments: installmentsByComma,
                },
              ),
          }),
        )
        .toPromise();

      this.deleteTransactionNextInstallmentsRequested.next(transaction);
    }
  }

  public getIconForTransaction(transaction: Transaction): string {
    switch (transaction.transactionType) {
      case TransactionType.CreditCard:
        return 'credit_card';
      case TransactionType.Expense:
        return 'trending_down';
      case TransactionType.Revenue:
        return 'trending_up';
      case TransactionType.Transfer:
        return 'sync_alt';
      case TransactionType.CreditCardBillPayment:
        return 'credit_score';
    }
  }

  public getTransactionTranslateKey(transaction: Transaction): string {
    switch (transaction.transactionType) {
      case TransactionType.CreditCard:
        return 'credit-card';
      case TransactionType.Expense:
        return 'expense';
      case TransactionType.Revenue:
        return 'revenue';
      case TransactionType.Transfer:
        return 'transfer';
      case TransactionType.CreditCardBillPayment:
        return 'credit-card-bill-payment';
    }
  }

  public getTransactionCssColor(transaction: Transaction): string {
    switch (transaction.transactionType) {
      case TransactionType.CreditCard:
        return 'credit-card-color';
      case TransactionType.Expense:
        return 'expense-color';
      case TransactionType.Revenue:
        return 'revenue-color';
      case TransactionType.Transfer:
        return 'transfer-color';
      case TransactionType.CreditCardBillPayment:
        return 'credit-card-bill-payment-color';
    }
  }

  public transactionIsInstallment(transaction: Transaction): boolean {
    return transaction.installment != null;
  }

  private showDeleteConfirmation(message: string): Promise<boolean> {
    return this.dialogService
      .openConfirm({
        title: this.translocoService.translate('confirm'),
        message: message,
        cancelButton: this.translocoService.translate('cancel'),
        acceptButton: this.translocoService.translate('delete'),
        width: '500px',
      })
      .afterClosed()
      .pipe(take(1))
      .toPromise();
  }
}

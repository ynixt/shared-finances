import { DOCUMENT } from '@angular/common';
import { Component, Inject, OnInit, Renderer2, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import moment, { Moment } from 'moment';
import { switchMap, take } from 'rxjs/operators';
import { BankAccount, Page, Transaction } from 'src/app/@core/models';
import { ErrorService, TransactionService } from 'src/app/@core/services';
import { NewTransactionDialogService } from 'src/app/components/new-transaction/new-transaction-dialog.service';
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

  private monthDate: Moment;

  constructor(
    private activatedRoute: ActivatedRoute,
    private bankAccountService: BankAccountService,
    private router: Router,
    private transactionService: TransactionService,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
    private newTransactionDialogService: NewTransactionDialogService,
    @Inject(DOCUMENT) private document: any,
    private renderer2: Renderer2,
  ) {}

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

  public async editTransaction(transaction: Transaction) {
    this.newTransactionDialogService.openDialog(this.document, this.renderer2, transaction.group != null, transaction);
  }

  public async deleteTransaction(transaction: Transaction) {
    await this.transactionService
      .deleteTransaction(transaction.id)
      .pipe(
        take(1),
        this.toast.observe({
          loading: this.translocoService.translate('deleting'),
          success: this.translocoService.translate('deleting-successful', { name: transaction.description }),
          error: error =>
            this.errorService.getInstantErrorMessage(error, 'deleting-error', 'deleting-error-with-description', {
              name: transaction.description,
            }),
        }),
      )
      .toPromise();
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

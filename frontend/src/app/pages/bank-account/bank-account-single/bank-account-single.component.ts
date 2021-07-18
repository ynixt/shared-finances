import { DOCUMENT } from '@angular/common';
import { Component, Inject, OnInit, Renderer2, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import moment, { Moment } from 'moment';
import { merge, Observable, Subscription } from 'rxjs';
import { switchMap, take } from 'rxjs/operators';
import { CHART_DEFAULT_MINIMUM_MONTHS } from 'src/app/@core/constants';
import { BankAccount, BankAccountSummary, Page, Transaction } from 'src/app/@core/models';
import { Chart } from 'src/app/@core/models/chart';
import { ErrorService, TransactionService } from 'src/app/@core/services';
import { DateUtil } from 'src/app/@core/util';
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
  transactionsGroupedYearMonth: Chart[];
  transactionsGroupedYearMonthExpenses: Chart[];
  transactionsGroupedYearMonthRevenues: Chart[];

  legend: boolean = true;
  showLabels: boolean = true;
  animations: boolean = true;
  showYAxisLabel: boolean = true;
  showXAxisLabel: boolean = true;

  disallowFutureOnSameMonth = true;

  colorScheme = {
    domain: ['#5AA454'],
  };
  colorSchemeExpense = {
    domain: ['#E44D25'],
  };

  bankAccount: BankAccount;
  pageSize = 20;
  transactionsPage$: Observable<Page<Transaction>>;
  bankAccountSummary: BankAccountSummary;

  private monthDate: Moment;
  private transactionsChangeSubscription: Subscription;

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

  public getTransactions(page = 1): void {
    this.transactionsPage$ = this.bankAccountService.getTransactions(
      this.bankAccount.id,
      { maxDate: moment(this.monthDate).endOf('month'), minDate: moment(this.monthDate).startOf('month') },
      { page, pageSize: this.pageSize },
    );
  }

  public async getBalance(): Promise<void> {
    const maxDate = this.getMaxDate();

    this.bankAccountSummary = undefined;
    this.bankAccountSummary = await this.bankAccountService.getBankAccountSummary(this.bankAccount.id, { maxDate: maxDate });
  }

  public async getChart(): Promise<void> {
    this.transactionsGroupedYearMonth = undefined;

    const bankAccountNamesById = new Map<string, string>();
    bankAccountNamesById.set(this.bankAccount.id, this.bankAccount.name);

    const charts = await this.bankAccountService.getTransactionsChart(bankAccountNamesById, this.getMaxDate().add(1), {
      bankAccountId: this.bankAccount.id,
      maxDate: this.getMaxDate(),
      minDate: moment(this.getMaxDate(false)).subtract(CHART_DEFAULT_MINIMUM_MONTHS, 'month'),
    });

    this.transactionsGroupedYearMonthExpenses = charts.filter(chart => chart.name.includes(this.translocoService.translate('expenses')));
    this.transactionsGroupedYearMonthRevenues = charts.filter(chart => chart.name.includes(this.translocoService.translate('revenues')));

    this.transactionsGroupedYearMonth = charts.filter(
      chart =>
        this.transactionsGroupedYearMonthExpenses.includes(chart) == false &&
        this.transactionsGroupedYearMonthRevenues.includes(chart) == false,
    );
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
          success: this.translocoService.translate('deleting-successful', { name: transaction.description ?? '' }),
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
    await this.getInfoBasedOnBankAndDate();
  }

  async toggleDisallowFutureOnSameMonth(): Promise<void> {
    this.disallowFutureOnSameMonth = !this.disallowFutureOnSameMonth;
    await this.getInfoBasedOnBankAndDate();
  }

  private async getBankAccount(bankAccount: BankAccount) {
    if (bankAccount == null) {
      this.router.navigateByUrl('/404');
      return;
    }

    this.bankAccount = bankAccount;
  }

  private getInfoBasedOnBankAndDate() {
    this.getTransactions();
    this.transactionsChange();
    return Promise.all([this.getBalance(), this.getChart()]);
  }

  private transactionsChange(): void {
    this.transactionsChangeSubscription?.unsubscribe();

    this.transactionsChangeSubscription = merge(
      this.bankAccountService.onTransactionCreated(this.bankAccount.id),
      this.bankAccountService.onTransactionUpdated(this.bankAccount.id),
      this.bankAccountService.onTransactionDeleted(this.bankAccount.id),
    )
      .pipe(untilDestroyed(this))
      .subscribe(async () => {
        await Promise.all([this.getBalance(), this.getChart()]);
      });
  }

  /**
   *
   * @param disallowFutureOnSameMonth If true AND 'monthDate' is the same month as the current month, the date that will be returned will be the current date.
   * @returns
   */
  private getMaxDate(disallowFutureOnSameMonth = this.disallowFutureOnSameMonth) {
    let maxDate = moment(this.monthDate).endOf('month');

    if (disallowFutureOnSameMonth && moment(this.monthDate).isSame(moment(), 'month') && DateUtil.dateIsBiggerThanToday(maxDate)) {
      maxDate = moment();
    }

    return maxDate;
  }
}

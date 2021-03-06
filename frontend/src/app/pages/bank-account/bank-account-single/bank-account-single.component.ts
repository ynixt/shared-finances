import { DOCUMENT } from '@angular/common';
import { Component, Inject, OnInit, Renderer2, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoService } from '@ngneat/transloco';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import moment, { Moment } from 'moment';
import { merge, Observable, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { CHART_DEFAULT_MINIMUM_MONTHS, DEFAULT_PAGE_SIZE } from 'src/app/@core/constants';
import { BankAccount, BankAccountSummary, Page, Transaction } from 'src/app/@core/models';
import { Chart } from 'src/app/@core/models/chart';
import { TransactionService, BankAccountService } from 'src/app/@core/services';
import { NewTransactionDialogService } from 'src/app/components/new-transaction/new-transaction-dialog.service';

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
  pageSize = DEFAULT_PAGE_SIZE;
  transactionsPage$: Observable<Page<Transaction>>;
  bankAccountSummaryState: { isLoading: boolean; summary?: BankAccountSummary } = {
    isLoading: true,
  };

  private monthDate: Moment;
  private transactionsChangeSubscription: Subscription;

  constructor(
    private activatedRoute: ActivatedRoute,
    private bankAccountService: BankAccountService,
    private router: Router,
    private transactionService: TransactionService,
    private translocoService: TranslocoService,
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

  public async getBankAccountSummary(): Promise<void> {
    const maxDate = this.getMaxDate();

    this.bankAccountSummaryState = {
      isLoading: true,
    };

    this.bankAccountSummaryState = {
      summary: await this.bankAccountService.getBankAccountSummary({
        bankAccountId: this.bankAccount.id,
        maxDate: maxDate,
      }),
      isLoading: false,
    };
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

    this.transactionsChangeObserver();
  }

  private getInfoBasedOnBankAndDate() {
    this.getTransactions();
    return Promise.all([this.getBankAccountSummary(), this.getChart()]);
  }

  private transactionsChangeObserver(): void {
    this.transactionsChangeSubscription?.unsubscribe();

    this.transactionsChangeSubscription = merge(
      this.bankAccountService.onTransactionCreated(this.bankAccount.id),
      this.bankAccountService.onTransactionUpdated(this.bankAccount.id),
      this.bankAccountService.onTransactionDeleted(this.bankAccount.id),
    )
      .pipe(untilDestroyed(this))
      .subscribe(async () => {
        await Promise.all([this.getBankAccountSummary(), this.getChart()]);
      });
  }

  private getMaxDate(disallowFutureOnSameMonth = this.disallowFutureOnSameMonth) {
    return this.transactionService.getMaxDate(this.monthDate, disallowFutureOnSameMonth);
  }
}

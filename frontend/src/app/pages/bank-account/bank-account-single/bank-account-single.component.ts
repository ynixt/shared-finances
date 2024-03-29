import { DOCUMENT } from "@angular/common";
import { Component, Inject, OnInit, Renderer2, ViewEncapsulation } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { TranslocoService } from "@ngneat/transloco";
import { UntilDestroy, untilDestroyed } from "@ngneat/until-destroy";
import moment, { Moment } from "moment";
import { lastValueFrom, merge, Observable, Subscription } from "rxjs";
import { switchMap, take } from "rxjs/operators";
import { CHART_DEFAULT_MINIMUM_MONTHS, DEFAULT_PAGE_SIZE } from "src/app/@core/constants";
import { BankAccount, BankAccountSummary, Category, Page, Transaction } from "src/app/@core/models";
import { Chart } from "src/app/@core/models/chart";
import { TransactionService, BankAccountService } from "src/app/@core/services";
import { NewTransactionDialogService } from "src/app/components/new-transaction/new-transaction-dialog.service";
import { Color } from "@swimlane/ngx-charts/lib/utils/color-sets";
import { UntypedFormControl, UntypedFormGroup, Validators } from "@angular/forms";

@UntilDestroy()
@Component({
  selector: "app-bank-account-single",
  templateUrl: "./bank-account-single.component.html",
  styleUrls: ["./bank-account-single.component.scss"],
  encapsulation: ViewEncapsulation.None
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

  colorScheme: Color = {
    name: "default",
    selectable: true,
    group: 0 as any,
    domain: [
      "#5AA454"
    ]
  };

  colorSchemeExpense: Color = {
    name: "expense",
    selectable: true,
    group: 0 as any,
    domain: [
      "#E44D25"
    ]
  };

  bankAccount: BankAccount;
  pageSize = DEFAULT_PAGE_SIZE;
  transactionsPage$: Observable<Page<Transaction>>;
  bankAccountSummaryState: { isLoading: boolean; summary?: BankAccountSummary } = {
    isLoading: true
  };

  filterFormGroup = new UntypedFormGroup({
    categories: new UntypedFormControl()
  });

  get filterCategories() {
    return this.filterFormGroup.value.categories?.map(category => category.id);
  }

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
    private renderer2: Renderer2
  ) {
  }

  ngOnInit(): void {
    this.activatedRoute.params
      .pipe(
        untilDestroyed(this),
        switchMap(params => {
          return this.bankAccountService.getBankAccount(params.bankAccountId);
        })
      )//
      .subscribe(bankAccount => this.getBankAccount(bankAccount));
  }

  public filter() {
    return this.getInfoBasedOnFilters();
  }

  public clearFilters() {
    this.filterFormGroup.reset();
    return this.filter();
  }

  public getTransactions(page = 0, pageSize: number = this.pageSize): void {
    this.pageSize = pageSize;

    this.transactionsPage$ = this.bankAccountService.getTransactions(
      this.bankAccount.id,
      {
        maxDate: this.getMaxDate(),
        minDate: moment(this.monthDate).startOf("month"),
        categoriesId: this.filterCategories
      },
      { page, size: this.pageSize }
    );
  }

  public async getBankAccountSummary(): Promise<void> {
    const maxDate = this.getMaxDate();
    const minDate = moment(maxDate).startOf("month");

    this.bankAccountSummaryState = {
      isLoading: true
    };

    this.bankAccountSummaryState = {
      summary: await this.bankAccountService.getBankAccountSummary({
        bankAccountId: this.bankAccount.id,
        minDate: minDate,
        maxDate: maxDate,
        categoriesId: this.filterCategories
      }),
      isLoading: false
    };
  }

  public async getChart(): Promise<void> {
    this.transactionsGroupedYearMonth = undefined;

    const charts = await this.bankAccountService.getTransactionsChart(this.bankAccount, this.getMaxDate().add(1), {
      bankAccountId: this.bankAccount.id,
      maxDate: this.getMaxDate(),
      minDate: moment(this.getMaxDate(false)).subtract(CHART_DEFAULT_MINIMUM_MONTHS, "month"),
      categoriesId: this.filterCategories
    });

    this.transactionsGroupedYearMonthExpenses = charts.filter(chart => chart.name.includes(this.translocoService.translate("expenses")));
    this.transactionsGroupedYearMonthRevenues = charts.filter(chart => chart.name.includes(this.translocoService.translate("revenues")));

    this.transactionsGroupedYearMonth = charts.filter(
      chart =>
        this.transactionsGroupedYearMonthExpenses.includes(chart) == false &&
        this.transactionsGroupedYearMonthRevenues.includes(chart) == false
    );
  }

  public async editTransaction(transaction: Transaction) {
    this.newTransactionDialogService.openDialog(this.document, this.renderer2, transaction.group != null, transaction);
  }

  public async dateChanged(newDate: Moment): Promise<void> {
    this.monthDate = newDate;
    await this.getInfoBasedOnFilters();
  }

  async toggleDisallowFutureOnSameMonth(): Promise<void> {
    this.disallowFutureOnSameMonth = !this.disallowFutureOnSameMonth;
    await this.getInfoBasedOnFilters();
  }

  private async getBankAccount(bankAccount: BankAccount) {
    if (bankAccount == null) {
      this.router.navigateByUrl("/404");
      return;
    }

    this.bankAccount = bankAccount;

    this.transactionsChangeObserver();
  }

  private getInfoBasedOnFilters() {
    this.getTransactions();
    return Promise.all([this.getBankAccountSummary(), this.getChart()]);
  }

  private transactionsChangeObserver(): void {
    this.transactionsChangeSubscription?.unsubscribe();

    this.transactionsChangeSubscription = merge(
      this.bankAccountService.onTransactionCreated(this.bankAccount.id),
      this.bankAccountService.onTransactionUpdated(this.bankAccount.id),
      this.bankAccountService.onTransactionDeleted(this.bankAccount.id)
    )
      .pipe(untilDestroyed(this))
      .subscribe(async () => {
        const transactionsPage = await lastValueFrom(this.transactionsPage$.pipe(take(1)));

        if (transactionsPage.number == 0) {
          this.getTransactions();
        } else {
          // TODO: ask to user if he wants update
        }

        await Promise.all([this.getBankAccountSummary(), this.getChart()]);
      });
  }

  private getMaxDate(disallowFutureOnSameMonth = this.disallowFutureOnSameMonth) {
    return this.transactionService.getMaxDate(this.monthDate, disallowFutureOnSameMonth);
  }
}

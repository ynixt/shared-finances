import { Component, Inject, OnInit, Renderer2 } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { UntilDestroy, untilDestroyed } from "@ngneat/until-destroy";
import moment, { Moment } from "moment";
import { switchMap, take } from "rxjs/operators";
import { CreditCard, CreditCardSummary, Page, Transaction } from "src/app/@core/models";
import { CreditCardService } from "../credit-card.service";
import { CreditCardService as CreditCardCoreService } from "src/app/@core/services"; // TODO change name of this service or join them
import { lastValueFrom, merge, Observable, Subscription } from "rxjs";
import { DOCUMENT } from "@angular/common";
import { NewTransactionDialogService } from "src/app/components/new-transaction/new-transaction-dialog.service";
import { CreditCardBillPaymentDialogService } from "src/app/components/credit-card-bill-payment";
import { Chart } from "src/app/@core/models/chart";
import { DEFAULT_PAGE_SIZE } from "src/app/@core/constants";
import { ISO_DATE_FORMAT } from "../../../moment-extension";
import { Color } from "@swimlane/ngx-charts/lib/utils/color-sets";
import { UntypedFormControl, UntypedFormGroup } from "@angular/forms";

@UntilDestroy()
@Component({
  selector: "app-credit-card-single",
  templateUrl: "./credit-card-single.component.html",
  styleUrls: ["./credit-card-single.component.scss"]
})
export class CreditCardSingleComponent implements OnInit {
  creditCard: CreditCard;
  billDateIndex: number;
  creditCardSummary: CreditCardSummary;
  limitAvailableLoading = false;
  transactionsGroupedYearMonth: Chart[];

  legend: boolean = true;
  showLabels: boolean = true;
  animations: boolean = true;
  showYAxisLabel: boolean = true;
  showXAxisLabel: boolean = true;

  colorScheme: Color = {
    name: "default",
    selectable: true,
    group: 0 as any,
    domain: [
      "#5AA454"
    ]
  };

  pageSize = DEFAULT_PAGE_SIZE;
  transactionsPage$: Observable<Page<Transaction>>;

  private monthDate: Moment | string;
  private billDateOfCurrentMonth: Moment;
  private transactionsChangeSubscription: Subscription;
  private creditCardSubscription: Subscription;

  filterFormGroup = new UntypedFormGroup({
    categories: new UntypedFormControl()
  });

  get filterCategories() {
    return this.filterFormGroup.value.categories?.map(category => category.id);
  }

  get isBillDateOfCurrentMonth() {
    return moment(this.monthDate).isSame(this.billDateOfCurrentMonth);
  }

  get closedBill() {
    return moment(this.monthDate).isBefore(this.billDateOfCurrentMonth);
  }

  constructor(
    private activatedRoute: ActivatedRoute,
    private creditCardService: CreditCardService,
    private creditCardCoreService: CreditCardCoreService,
    private newTransactionDialogService: NewTransactionDialogService,
    @Inject(DOCUMENT) private document: any,
    private renderer2: Renderer2,
    private creditCardBillPaymentDialogService: CreditCardBillPaymentDialogService,
    private router: Router
  ) {
  }

  ngOnInit(): void {
    this.loadCreditCard();
  }

  loadCreditCard() {
    this.creditCardSubscription?.unsubscribe();

    this.creditCardSubscription = this.activatedRoute.params
      .pipe(
        untilDestroyed(this),
        switchMap(params => {
          return this.creditCardService.getById(params.creditCardId);
        })
      )
      .subscribe(creditCard => this.getCreditCardInfo(creditCard));
  }

  public filter() {
    return this.getInfoBasedOnFilters();
  }

  public clearFilters() {
    this.filterFormGroup.reset();
    return this.filter();
  }

  async getCreditCardInfo(creditCard: CreditCard): Promise<void> {
    this.creditCard = creditCard;

    if (this.creditCard == null) {
      this.router.navigateByUrl("/404");
    } else {
      this.setBillDateOfCurrentDate();
      this.getInfoBasedOnCreditCard();
      await this.getInfoBasedOnFilters();
    }
  }

  formatValue(date: string): string {
    return moment(date).format("L");
  }

  async dateChanged(newDate: Moment): Promise<void> {
    this.monthDate = newDate;
    await this.getInfoBasedOnFilters();
  }

  getTransactions(page = 0): void {
    this.transactionsPage$ = this.creditCardService.getTransactions(
      this.creditCard.id,
      {
        creditCardBillDate: moment(this.monthDate),
        categoriesId: this.filterCategories
      },
      { page, size: this.pageSize }
    );
  }

  public async editTransaction(transaction: Transaction) {
    this.newTransactionDialogService.openDialog(this.document, this.renderer2, transaction.group != null, transaction);
  }

  openPayBillDialog(): void {
    this.creditCardBillPaymentDialogService.openDialog(this.document, this.renderer2, {
      creditCard: this.creditCard,
      creditCardBillDate: moment(this.monthDate).startOf("day"),
      billValue: this.creditCardSummary.bill
    });
  }

  openNewTransactionDialog(): void {
    this.newTransactionDialogService.openDialog(this.document, this.renderer2, false);
  }

  async getChart(): Promise<void> {
    this.transactionsGroupedYearMonth = undefined;

    if (!this.creditCard.billDates || this.creditCard.billDates.length === 0) {
      return;
    }

    const charts = await this.creditCardCoreService.getTransactionsChart(
      this.creditCard, this.monthDate, this.creditCard.closingDay, {
        maxCreditCardBillDate: moment(this.monthDate, ISO_DATE_FORMAT),
        minCreditCardBillDate: moment(this.creditCard.billDatesValue[0], ISO_DATE_FORMAT),
        categoriesId: this.filterCategories
      }
    );

    this.transactionsGroupedYearMonth = charts;
  }

  private getInfoBasedOnCreditCard(): void {
    this.transactionsChange();
  }

  private async getInfoBasedOnFilters(): Promise<void> {
    this.getTransactions();
    await Promise.all([this.getCreditCardSummary(), this.getChart()]);
  }

  private async getCreditCardSummary(): Promise<void> {
    this.creditCardSummary = undefined;
    this.creditCardSummary = (await this.creditCardCoreService.getCreditCardSummary(this.creditCard.id, this.monthDate, this.filterCategories)) ?? { bill: 0 };
  }

  private setBillDateOfCurrentDate() {
    this.insertCurrentMonthOnBillDatesIfMissing();
    this.insertNextMonthOnBillDatesIfLastIsCurrent();

    this.billDateOfCurrentMonth = this.creditCardCoreService.findCreditCardBillDate(
      moment(),
      this.creditCard.billDatesValue,
      this.creditCard.closingDay
    );

    if (this.billDateOfCurrentMonth) {
      this.billDateIndex = this.creditCard.billDatesValue.indexOf(this.billDateOfCurrentMonth.format(ISO_DATE_FORMAT));
    }

    this.monthDate = this.billDateOfCurrentMonth;
  }

  private insertCurrentMonthOnBillDatesIfMissing() {
    if (
      this.creditCard.billDatesValue?.length > 0 &&
      this.creditCard.billDatesValue.find(billDate => moment(billDate, ISO_DATE_FORMAT).isSame(moment(), "month")) == null
    ) {
      this.creditCard.billDatesValue = [
        ...this.creditCard.billDatesValue,
        this.creditCardCoreService.nextBillDate(moment(), this.creditCard.closingDay, 0).format(ISO_DATE_FORMAT)
      ].sort((billDateA, billDateB) => moment(
        billDateA, ISO_DATE_FORMAT).format("X"
      ).localeCompare(moment(billDateB, ISO_DATE_FORMAT).format("X")));
    }
  }

  private insertNextMonthOnBillDatesIfLastIsCurrent() {
    if (
      this.creditCard.billDatesValue?.length > 0 &&
      moment(
        this.creditCard.billDatesValue[this.creditCard.billDatesValue.length - 1], ISO_DATE_FORMAT
      ).isSame(moment(), "month")
    ) {
      this.creditCard.billDatesValue = [
        ...this.creditCard.billDatesValue,
        this.creditCardCoreService.nextBillDate(moment(), this.creditCard.closingDay).toISOString()
      ];
    }
  }

  private transactionsChange(): void {
    this.transactionsChangeSubscription?.unsubscribe();

    this.transactionsChangeSubscription = merge(
      this.creditCardCoreService.onTransactionCreated(this.creditCard.id),
      this.creditCardCoreService.onTransactionUpdated(this.creditCard.id),
      this.creditCardCoreService.onTransactionDeleted(this.creditCard.id)
    )
      .pipe(untilDestroyed(this))
      .subscribe(async () => {
        const transactionsPage = await lastValueFrom(this.transactionsPage$.pipe(take(1)));

        if (transactionsPage.number == 0) {
          this.getTransactions();
        } else {
          // TODO: ask to user if he wants update
        }

        if (this.creditCard.billDates?.length > 0) {
          await Promise.all([this.getCreditCardSummary(), this.getChart(), this.reloadAvailableLimit()]);
        } else {
          await this.loadCreditCard();
        }
      });
  }

  private async reloadAvailableLimit(): Promise<void> {
    this.limitAvailableLoading = true;

    const newAvaiableLimit = await this.creditCardCoreService.getCreditCardSAvailableLimit(this.creditCard.id);
    this.creditCard.availableLimit = newAvaiableLimit;

    this.limitAvailableLoading = false;
  }
}

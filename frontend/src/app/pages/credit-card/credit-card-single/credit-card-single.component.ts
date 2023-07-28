import { Component, Inject, OnInit, Renderer2 } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import moment, { Moment } from 'moment';
import { switchMap } from 'rxjs/operators';
import { CreditCard, CreditCardSummary, Page, Transaction } from 'src/app/@core/models';
import { CreditCardService } from '../credit-card.service';
import { CreditCardService as CreditCardCoreService } from 'src/app/@core/services'; // TODO change name of this service or join them
import { merge, Observable, Subscription } from 'rxjs';
import { DOCUMENT } from '@angular/common';
import { NewTransactionDialogService } from 'src/app/components/new-transaction/new-transaction-dialog.service';
import { CreditCardBillPaymentDialogService } from 'src/app/components/credit-card-bill-payment';
import { Chart } from 'src/app/@core/models/chart';
import { DEFAULT_PAGE_SIZE } from 'src/app/@core/constants';

@UntilDestroy()
@Component({
  selector: 'app-credit-card-single',
  templateUrl: './credit-card-single.component.html',
  styleUrls: ['./credit-card-single.component.scss'],
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

  colorScheme = '#5AA454'

  pageSize = DEFAULT_PAGE_SIZE;
  transactionsPage$: Observable<Page<Transaction>>;

  private monthDate: Moment | string;
  private billDateOfCurrentMonth: Moment;
  private transactionsChangeSubscription: Subscription;
  private creditCardSubscription: Subscription;

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
    private router: Router,
  ) {}

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
        }),
      )
      .subscribe(creditCard => this.getCreditCardInfo(creditCard));
  }

  async getCreditCardInfo(creditCard: CreditCard): Promise<void> {
    this.creditCard = creditCard;

    if (this.creditCard == null) {
      this.router.navigateByUrl('/404');
    } else {
      this.setBillDateOfCurrentDate();
      this.getInfoBasedOnCreditCard();
      await this.getInfoBasedOnCreditCardAndDate();
    }
  }

  formatValue(date: string): string {
    return moment(date).format('L');
  }

  async dateChanged(newDate: Moment): Promise<void> {
    this.monthDate = newDate;
    await this.getInfoBasedOnCreditCardAndDate();
  }

  getTransactions(page = 1): void {
    this.transactionsPage$ = this.creditCardService.getTransactions(
      this.creditCard.id,
      {
        creditCardBillDate: moment(this.monthDate),
      },
      { page, size: this.pageSize },
    );
  }

  public async editTransaction(transaction: Transaction) {
    this.newTransactionDialogService.openDialog(this.document, this.renderer2, transaction.group != null, transaction);
  }

  openPayBillDialog(): void {
    this.creditCardBillPaymentDialogService.openDialog(this.document, this.renderer2, {
      creditCard: this.creditCard,
      creditCardBillDate: moment(this.monthDate).startOf('day'),
      billValue: this.creditCardSummary.bill,
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

    const creditCardNamesById = new Map<string, string>();
    creditCardNamesById.set(this.creditCard.id, this.creditCard.name);

    const charts = await this.creditCardCoreService.getTransactionsChart(creditCardNamesById, this.monthDate, this.creditCard.closingDay, {
      creditCardId: this.creditCard.id,
      maxCreditCardBillDate: moment(this.monthDate),
      minCreditCardBillDate: moment(this.creditCard.billDates[0]),
    });

    this.transactionsGroupedYearMonth = charts;
  }

  private getInfoBasedOnCreditCard(): void {
    this.transactionsChange();
  }

  private async getInfoBasedOnCreditCardAndDate(): Promise<void> {
    this.getTransactions();
    await Promise.all([this.getCreditCardSummary(), this.getChart()]);
  }

  private async getCreditCardSummary(): Promise<void> {
    this.creditCardSummary = undefined;
    this.creditCardSummary = (await this.creditCardCoreService.getCreditCardSummary(this.creditCard.id, this.monthDate)) ?? { bill: 0 };
  }

  private setBillDateOfCurrentDate() {
    this.insertCurrentMonthOnBillDatesIfMissing();
    this.insertNextMonthOnBillDatesIfLastIsCurrent();

    this.billDateOfCurrentMonth = this.creditCardCoreService.findCreditCardBillDate(
      moment(),
      this.creditCard.billDates,
      this.creditCard.closingDay,
    );

    if (this.billDateOfCurrentMonth) {
      this.billDateIndex = this.creditCard.billDates.indexOf(this.billDateOfCurrentMonth.toISOString());
    }

    this.monthDate = this.billDateOfCurrentMonth;
  }

  private insertCurrentMonthOnBillDatesIfMissing() {
    if (
      this.creditCard.billDates?.length > 0 &&
      this.creditCard.billDates.find(billDate => moment(billDate).isSame(moment(), 'month')) == null
    ) {
      this.creditCard.billDates = [
        ...this.creditCard.billDates,
        this.creditCardCoreService.nextBillDate(moment(), this.creditCard.closingDay, 0).toISOString(),
      ].sort((billDateA, billDateB) => moment(billDateA).format('X').localeCompare(moment(billDateB).format('X')));
    }
  }

  private insertNextMonthOnBillDatesIfLastIsCurrent() {
    if (
      this.creditCard.billDates?.length > 0 &&
      moment(this.creditCard.billDates[this.creditCard.billDates.length - 1]).isSame(moment(), 'month')
    ) {
      this.creditCard.billDates = [
        ...this.creditCard.billDates,
        this.creditCardCoreService.nextBillDate(moment(), this.creditCard.closingDay).toISOString(),
      ];
    }
  }

  private transactionsChange(): void {
    this.transactionsChangeSubscription?.unsubscribe();

    this.transactionsChangeSubscription = merge(
      this.creditCardCoreService.onTransactionCreated(this.creditCard.id),
      this.creditCardCoreService.onTransactionUpdated(this.creditCard.id),
      this.creditCardCoreService.onTransactionDeleted(this.creditCard.id),
    )
      .pipe(untilDestroyed(this))
      .subscribe(async () => {
        if (this.creditCard.billDates?.length > 0) {
          await Promise.all([this.getInfoBasedOnCreditCardAndDate(), this.reloadAvailableLimit()]);
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

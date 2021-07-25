import { Component, Inject, OnInit, Renderer2 } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import moment, { Moment } from 'moment';
import { switchMap, take } from 'rxjs/operators';
import { CreditCard, CreditCardSummary, Page, Transaction } from 'src/app/@core/models';
import { CreditCardService } from '../credit-card.service';
import { CreditCardService as CreditCardCoreService, ErrorService, TransactionService } from 'src/app/@core/services'; // TODO change name of this service or join them
import { merge, Observable, Subscription } from 'rxjs';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { DOCUMENT } from '@angular/common';
import { NewTransactionDialogService } from 'src/app/components/new-transaction/new-transaction-dialog.service';
import { CreditCardBillPaymentDialogService } from 'src/app/components/credit-card-bill-payment';
import { Chart } from 'src/app/@core/models/chart';

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

  colorScheme = {
    domain: ['#5AA454'],
  };

  pageSize = 20;
  transactionsPage$: Observable<Page<Transaction>>;

  private monthDate: Moment | string;
  private billDateOfCurrentMonth: Moment;
  private transactionsChangeSubscription: Subscription;
  private creditCardSubscription: Subscription;

  get closedBill() {
    return moment(this.monthDate).isSame(this.billDateOfCurrentMonth) === false;
  }

  constructor(
    private activatedRoute: ActivatedRoute,
    private creditCardService: CreditCardService,
    private creditCardCoreService: CreditCardCoreService,
    private transactionService: TransactionService,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
    private newTransactionDialogService: NewTransactionDialogService,
    @Inject(DOCUMENT) private document: any,
    private renderer2: Renderer2,
    private creditCardBillPaymentDialogService: CreditCardBillPaymentDialogService,
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

    this.setBillDateOfCurrentDate();
    this.getInfoBasedOnCreditCard();
    await this.getInfoBasedOnCreditCardAndDate();
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
      { page, pageSize: this.pageSize },
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
    if (
      this.creditCard.billDates?.length > 0 &&
      moment(this.creditCard.billDates[this.creditCard.billDates.length - 1]).isSame(moment(), 'month')
    ) {
      this.creditCard.billDates = [
        ...this.creditCard.billDates,
        this.creditCardCoreService.nextBillDate(moment(), this.creditCard.closingDay).toISOString(),
      ];
    }

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

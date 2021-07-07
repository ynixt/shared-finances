import { Component, Inject, OnInit, Renderer2 } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import moment, { Moment } from 'moment';
import { switchMap, take } from 'rxjs/operators';
import { CreditCard, Page, Transaction } from 'src/app/@core/models';
import { CreditCardService } from '../credit-card.service';
import { CreditCardService as CreditCardCoreService, ErrorService, TransactionService } from 'src/app/@core/services'; // TODO change name of this service or join them
import { merge, Observable, Subscription } from 'rxjs';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { DOCUMENT } from '@angular/common';
import { NewTransactionDialogService } from 'src/app/components/new-transaction/new-transaction-dialog.service';

@UntilDestroy()
@Component({
  selector: 'app-credit-card-single',
  templateUrl: './credit-card-single.component.html',
  styleUrls: ['./credit-card-single.component.scss'],
})
export class CreditCardSingleComponent implements OnInit {
  creditCard: CreditCard;
  billDateIndex: number;

  pageSize = 20;
  transactionsPage$: Observable<Page<Transaction>>;

  private monthDate: Moment;
  private transactionsChangeSubscription: Subscription;

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
  ) {}

  ngOnInit(): void {
    this.activatedRoute.params
      .pipe(
        untilDestroyed(this),
        switchMap(params => {
          return this.creditCardService.getById(params.creditCardId);
        }),
      )
      .subscribe(creditCard => this.getCreditCard(creditCard));
  }

  getCreditCard(creditCard: CreditCard) {
    this.creditCard = creditCard;

    this.setBillDateOfCurrentDate();
    this.getInfoBasedOnCreditCardAndDate();
  }

  formatValue(date: string): string {
    return moment(date).format('L');
  }

  async dateChanged(newDate: Moment): Promise<void> {
    this.monthDate = newDate;
    this.getInfoBasedOnCreditCardAndDate();
  }

  getTransactions(page = 1): void {
    this.transactionsPage$ = this.creditCardService.getTransactions(
      this.creditCard.id,
      {
        maxDate: moment(this.monthDate).startOf('day'),
        minDate: this.creditCardCoreService.previousBillDate(moment(this.monthDate), this.creditCard.closingDay).endOf('day'),
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
          success: this.translocoService.translate('deleting-successful', { name: transaction.description }),
          error: error =>
            this.errorService.getInstantErrorMessage(error, 'deleting-error', 'deleting-error-with-description', {
              name: transaction.description,
            }),
        }),
      )
      .toPromise();
  }

  private getInfoBasedOnCreditCardAndDate(): void {
    this.getTransactions();
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

    const billDateOfCurrentDate = this.creditCardCoreService.findCreditCardBillDate(
      moment(),
      this.creditCard.billDates,
      this.creditCard.closingDay,
    );

    this.billDateIndex = this.creditCard.billDates.indexOf(billDateOfCurrentDate.toISOString());
    this.monthDate = billDateOfCurrentDate;
  }

  // private transactionsChange(): void {
  //   this.transactionsChangeSubscription?.unsubscribe();

  //   this.transactionsChangeSubscription = merge(
  //     this.bankAccountService.onTransactionCreated(this.bankAccount.id),
  //     this.bankAccountService.onTransactionUpdated(this.bankAccount.id),
  //     this.bankAccountService.onTransactionDeleted(this.bankAccount.id),
  //   )
  //     .pipe(untilDestroyed(this))
  //     .subscribe(async () => {
  //       await Promise.all([this.getBalance(), this.getChart()]);
  //     });
  // }
}

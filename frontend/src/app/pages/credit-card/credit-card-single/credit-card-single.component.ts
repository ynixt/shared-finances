import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import moment, { Moment } from 'moment';
import { switchMap } from 'rxjs/operators';
import { CreditCard } from 'src/app/@core/models';
import { CreditCardService } from '../credit-card.service';
import { CreditCardService as CreditCardCoreService } from 'src/app/@core/services'; // TODO change name of this service or join them

@UntilDestroy()
@Component({
  selector: 'app-credit-card-single',
  templateUrl: './credit-card-single.component.html',
  styleUrls: ['./credit-card-single.component.scss'],
})
export class CreditCardSingleComponent implements OnInit {
  creditCard: CreditCard;
  billDateIndex: number;

  constructor(
    private activatedRoute: ActivatedRoute,
    private creditCardService: CreditCardService,
    private creditCardCoreService: CreditCardCoreService,
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
  }

  formatValue(date: string): string {
    return moment(date).format('L');
  }

  async dateChanged(newDate: Moment): Promise<void> {}

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
    console.log(this.billDateIndex);
  }
}

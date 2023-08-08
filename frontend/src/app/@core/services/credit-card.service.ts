import { Injectable } from "@angular/core";
import moment, { Moment } from "moment";
import { lastValueFrom, Observable } from "rxjs";
import { map, startWith, switchMap, take } from "rxjs/operators";
import { CHART_DEFAULT_MINIMUM_MONTHS } from "../constants";

import { BankAccount, CreditCard, CreditCardSummary } from "../models";
import { Chart, ChartSerie } from "../models/chart";
import { HttpClient } from "@angular/common/http";
import { StompService } from "./stomp.service";
import { ISO_DATE_FORMAT } from "../../moment-extension";
import { addHttpParamsIntoUrl } from "../util";
import { TransactionValuesAndDateDto } from "../models/transaction-values-and-date";
import { TranslocoService } from "@ngneat/transloco";

@Injectable({
  providedIn: "root"
})
export class CreditCardService {
  constructor(private httpClient: HttpClient, private stompService: StompService, private translocoService: TranslocoService) {
  }

  watchCreditCards(): Observable<CreditCard[]> {
    return this.httpClient.get<CreditCard[]>(`/api/credit-card`).pipe(
      switchMap(cats => this.stompService.watch({
          destination: "/user/queue/credit-card"
        }).pipe(map(message => JSON.parse(message.body) as CreditCard[]), startWith(cats))
      )
    );
  }

  getCreditCardSummary(creditCardId: string, maxCreditCardBillDate: string | Moment, categoriesId?: string[]): Promise<CreditCardSummary> {
    const maxCreditCardBillDateFormatted = moment(maxCreditCardBillDate).utc().format(ISO_DATE_FORMAT);
    const url = addHttpParamsIntoUrl(`/api/credit-card/summary/${creditCardId}/${maxCreditCardBillDateFormatted}`, {
      categoriesId: categoriesId
    });

    return lastValueFrom(this.httpClient.get<CreditCardSummary>(url).pipe(take(1)));
  }

  getCreditCardSAvailableLimit(creditCardId: string): Promise<number> {
    return lastValueFrom(this.httpClient.get<{ id: string, limit: number, availableLimit: number }>(
      `/api/credit-card/${creditCardId}/limit`
    ).pipe(take(1), map(response => response.availableLimit)));
  }

  onTransactionCreated(creditCardId: string): Observable<string> {
    return this.stompService.watch({
      destination: "/user/queue/credit-card/transaction-created/" + creditCardId
    }).pipe(
      map(message => JSON.parse(message.body) as CreditCard),
      map(creditCard => creditCard.id)
    );
  }

  onTransactionUpdated(creditCardId: string): Observable<string> {
    return this.stompService.watch({
      destination: "/user/queue/credit-card/transaction-updated/" + creditCardId
    }).pipe(
      map(message => JSON.parse(message.body) as CreditCard),
      map(creditCard => creditCard.id)
    );
  }

  onTransactionDeleted(creditCardId: string): Observable<string> {
    return this.stompService.watch({
      destination: "/user/queue/credit-card/transaction-deleted/" + creditCardId
    }).pipe(
      map(message => JSON.parse(message.body) as CreditCard),
      map(creditCard => creditCard.id)
    );
  }

  async getTransactionsChart(
    creditCard: CreditCard,
    initialMonthIfNoChart: Moment | string,
    creditCardClosingDay: number,
    args: { maxCreditCardBillDate?: Moment; minCreditCardBillDate?: Moment, categoriesId: string[] },
    minimumMonths = CHART_DEFAULT_MINIMUM_MONTHS
  ): Promise<Chart[]> {
    const url = addHttpParamsIntoUrl(`/api/credit-card/${creditCard.id}/chart`, {
      maxCreditCardBillDate: args?.maxCreditCardBillDate?.format(ISO_DATE_FORMAT),
      minCreditCardBillDate: args?.minCreditCardBillDate?.format(ISO_DATE_FORMAT),
      categoriesId: args?.categoriesId
    });

    const values = await lastValueFrom(
      this.httpClient.get<TransactionValuesAndDateDto[]>(url).pipe(take(1))
    );

    const charts: Chart[] = [];
    const dateFormat = this.translocoService.translate("date-format.month-year");
    const dateFormatFromServer = "YYYY-MM";

    charts.push(new Chart({
      name: creditCard.name,
      series: values.map(v => new ChartSerie({
        name: moment(v.date, dateFormatFromServer).format(dateFormat),
        value: v.balance * -1
      }))
    }));

    charts.forEach(chart => {
      if (chart.series.length < minimumMonths) {
        const missing = minimumMonths - chart.series.length;
        const firstDate = chart.series?.length > 0 ? chart.series[0].name : initialMonthIfNoChart;

        for (let i = 0; i < missing; i++) {
          chart.series.splice(i, 0, {
            name: this.previousBillDate(moment(firstDate, dateFormat), creditCardClosingDay, i + 1).format(dateFormat),
            value: 0
          });
        }
      }

      chart.series.sort((b1, b2) => {
        const b1Str = moment(b1.name, dateFormat).toISOString();
        const b2Str = moment(b2.name, dateFormat).toISOString();

        return b1Str.localeCompare(b2Str);
      });
    });

    return charts;
  }

  nextBillDate(date: string | Moment, closingDay: number, amountAhead = 1): Moment {
    const dateForThisOption = moment(date, ISO_DATE_FORMAT).date(closingDay);

    if (dateForThisOption.isSame(date, "month") === false && amountAhead != 0) {
      // month with less days, like february

      dateForThisOption.subtract(1, "day");
    }

    return dateForThisOption.add(amountAhead, "month");
  }

  previousBillDate(date: string | Moment, closingDay: number, amountAhead = 1): Moment {
    const dateForThisOption = moment(date, ISO_DATE_FORMAT).date(closingDay);

    return dateForThisOption.subtract(amountAhead, "month");
  }

  findCreditCardBillDate(date: string | Moment, creditCardBillDateOptions: Array<Moment | string>, closingDay: number): Moment | undefined {
    let oneMonthSkipped = creditCardBillDateOptions.length <= 1;

    creditCardBillDateOptions = [...creditCardBillDateOptions];

    return creditCardBillDateOptions
      .sort((b1, b2) => {
        const b1Str = typeof b1 === "string" ? b1 : b1.format(ISO_DATE_FORMAT);
        const b2Str = typeof b2 === "string" ? b2 : b2.format(ISO_DATE_FORMAT);

        return b1Str.localeCompare(b2Str);
      })
      .map(option => moment(option, ISO_DATE_FORMAT))
      .find(option => {
        const isSameMonth = moment(date, ISO_DATE_FORMAT).isSame(option, "month");
        const isSameOrAfterClosingDay = moment(date, ISO_DATE_FORMAT).date() >= closingDay;

        if (isSameMonth || oneMonthSkipped) {
          if (isSameOrAfterClosingDay && !oneMonthSkipped) {
            oneMonthSkipped = true;
            return false;
          } else {
            return true;
          }
        }

        return false;
      });
  }
}

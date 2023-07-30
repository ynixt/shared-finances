import { Injectable } from "@angular/core";
import { TranslocoService } from "@ngneat/transloco";
import moment, { Moment } from "moment";
import { lastValueFrom, Observable } from "rxjs";
import { map, take } from "rxjs/operators";
import { CHART_DEFAULT_MINIMUM_MONTHS } from "src/app/@core/constants";
import { BankAccount, BankAccountSummary, Page, Pagination, Transaction } from "src/app/@core/models";
import { Chart, ChartSerie } from "src/app/@core/models/chart";
import { StompService } from "./stomp.service";
import { HttpClient } from "@angular/common/http";
import { ISO_DATE_FORMAT } from "../../moment-extension";
import { TransactionValuesAndDateDto } from "../models/transaction-values-and-date";
import { addHttpParamsIntoUrl } from "../util";

@Injectable({
  providedIn: "root"
})
export class BankAccountService {
  constructor(private translocoService: TranslocoService, private stompService: StompService, private httpClient: HttpClient) {
  }

  getBankAccount(bankAccountId: string): Observable<BankAccount> {
    const w = this.stompService.watch({
      destination: `/user/queue/bank-account/${bankAccountId}`
    });

    this.stompService.publish({ destination: `/app/bank-account/${bankAccountId}` });

    return w.pipe(map(message => JSON.parse(message.body) as BankAccount));
  }

  onTransactionCreated(bankAccountId?: string): Observable<string> {
    return this.stompService.watch({
      destination: "/user/queue/bank-account/transaction-created/" + bankAccountId ?? ""
    }).pipe(
      map(message => JSON.parse(message.body) as BankAccount),
      map(bank => bank.id)
    );
  }

  onTransactionUpdated(bankAccountId?: string): Observable<string> {
    return this.stompService.watch({
      destination: "/user/queue/bank-account/transaction-updated/" + bankAccountId ?? ""
    }).pipe(
      map(message => JSON.parse(message.body) as BankAccount),
      map(bank => bank.id)
    );
  }

  onTransactionDeleted(bankAccountId?: string): Observable<string> {
    return this.stompService.watch({
      destination: "/user/queue/bank-account/transaction-deleted/" + bankAccountId ?? ""
    }).pipe(
      map(message => JSON.parse(message.body) as BankAccount),
      map(bank => bank.id)
    );
  }

  getTransactions(
    bankAccountId: string,
    args: { maxDate: Moment; minDate: Moment },
    pagination?: Pagination
  ): Observable<Page<Transaction>> {
    const url = addHttpParamsIntoUrl(`/api/bank-account/${bankAccountId}/transactions`, {
      page: pagination?.page,
      size: pagination?.size,
      maxDate: args?.maxDate?.format(ISO_DATE_FORMAT),
      minDate: args?.minDate?.format(ISO_DATE_FORMAT)
    })

    return this.httpClient.get<Page<Transaction>>(url);
  }

  getBankAccountSummary(obj?: { maxDate?: Moment; bankAccountId?: string }): Promise<BankAccountSummary> {
    const url = addHttpParamsIntoUrl('/api/bank-account/summary', {
      bankAccountId: obj?.bankAccountId,
      maxDate: obj?.maxDate?.format(ISO_DATE_FORMAT)
    })

    return lastValueFrom(this.httpClient.get<BankAccountSummary>(url).pipe(take(1)));
  }

  newBankAccount(bankAccount: Partial<BankAccount>): Observable<BankAccount> {
    return this.httpClient.post<BankAccount>("/api/bank-account", {
      name: bankAccount.name,
      enabled: bankAccount.enabled,
      displayOnGroup: bankAccount.displayOnGroup
    });
  }

  changeBankAccountName(bankAccountId: string, name: string): Observable<{ name: string }> {
    return this.httpClient.put<BankAccount>(
      `/api/bank-account/${bankAccountId}/${name}`, null
    )
      .pipe(
        map(bankAccount => ({ name: bankAccount.name }))
      );
  }

  deleteBankAccount(bankAccountId: string): Observable<void> {
    return this.httpClient.delete<void>(`/api/bank-account/${bankAccountId}`);
  }

  async getTransactionsChart(
    bankAccount: BankAccount,
    initialMonthIfNoChart: Moment | string,
    args?: { bankAccountId: string; maxDate?: Moment; minDate?: Moment },
    minimumMonths = CHART_DEFAULT_MINIMUM_MONTHS
  ): Promise<Chart[]> {
    const url = addHttpParamsIntoUrl(`/api/bank-account/${bankAccount.id}/chart`, {
      maxDate: args?.maxDate?.format(ISO_DATE_FORMAT),
      minDate: args?.minDate?.format(ISO_DATE_FORMAT)
    })

    const values = await lastValueFrom(
      this.httpClient.get<TransactionValuesAndDateDto[]>(url).pipe(take(1))
    );

    const charts: Chart[] = [];
    const dateFormat = this.translocoService.translate("date-format.month-year");
    const dateFormatFromServer = "YYYY-MM";

    charts.push(new Chart({
      name: `${bankAccount.name} - ${this.translocoService.translate("balance")}`,
      series: values.map(v => new ChartSerie({
        name: moment(v.date, dateFormatFromServer).format(dateFormat),
        value: v.balance
      }))
    }));

    charts.push(new Chart({
      name: `${bankAccount.name} - ${this.translocoService.translate("revenues")}`,
      series: values.map(v => new ChartSerie({
        name: moment(v.date, dateFormatFromServer).format(dateFormat),
        value: v.revenues
      }))
    }));

    charts.push(new Chart({
      name: `${bankAccount.name} - ${this.translocoService.translate("expenses")}`,
      series: values.map(v => new ChartSerie({
        name: moment(v.date, dateFormatFromServer).format(dateFormat),
        value: v.expenses
      }))
    }));

    charts.forEach(chart => {
      if (chart.series.length < minimumMonths) {
        const missing = minimumMonths - chart.series.length;
        const firstDate = chart.series?.length > 0 ? chart.series[0].name : initialMonthIfNoChart;

        for (let i = 0; i < missing; i++) {
          chart.series.splice(i, 0, {
            name: moment(firstDate, dateFormat)
              .subtract(missing - i, "month")
              .format(dateFormat),
            value: 0
          });
        }
      }
    });

    return charts;
  }
}

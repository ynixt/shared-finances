import { Injectable } from "@angular/core";
import { Moment } from "moment";
import { lastValueFrom, Observable } from "rxjs";
import { take } from "rxjs/operators";
import { CreditCard, Page, Pagination, Transaction } from "src/app/@core/models";
import { HttpClient } from "@angular/common/http";
import { addHttpParamsIntoUrl } from "../../@core/util";
import { ISO_DATE_FORMAT } from "../../moment-extension";

@Injectable({
  providedIn: "root"
})
export class CreditCardService {
  constructor(private httpClient: HttpClient) {
  }

  newCreditCard(creditCard: Partial<CreditCard>): Observable<CreditCard> {
    return this.httpClient.post<CreditCard>("/api/credit-card", {
      name: creditCard.name,
      limit: creditCard.limit,
      closingDay: creditCard.closingDay,
      paymentDay: creditCard.paymentDay,
      enabled: creditCard.enabled,
      displayOnGroup: creditCard.displayOnGroup
    });
  }

  editCreditCard(creditCard: CreditCard): Observable<CreditCard> {
    return this.httpClient.put<CreditCard>(`/api/credit-card/${creditCard.id}`, {
      name: creditCard.name,
      limit: creditCard.limit,
      closingDay: creditCard.closingDay,
      paymentDay: creditCard.paymentDay,
      enabled: creditCard.enabled,
      displayOnGroup: creditCard.displayOnGroup
    });
  }

  getById(creditCardId: string): Promise<CreditCard> {
    return lastValueFrom(this.httpClient.get<CreditCard>(`/api/credit-card/${creditCardId}`).pipe(take(1)));
  }

  deleteCreditCard(creditCardId: string): Observable<void> {
    return this.httpClient.delete<void>(`/api/credit-card/${creditCardId}`);
  }

  getTransactions(
    creditCardId: string,
    args?: { maxDate?: Moment; minDate?: Moment; creditCardBillDate: Moment, categoriesId: string[] },
    pagination?: Pagination
  ): Observable<Page<Transaction>> {
    const url = addHttpParamsIntoUrl(`/api/credit-card/${creditCardId}/transactions`, {
      page: pagination?.page,
      size: pagination?.size,
      maxDate: args?.maxDate?.format(ISO_DATE_FORMAT),
      minDate: args?.minDate?.format(ISO_DATE_FORMAT),
      creditCardBillDate: args?.creditCardBillDate?.format(ISO_DATE_FORMAT),
      categoriesId: args?.categoriesId
    });

    return this.httpClient.get<Page<Transaction>>(url);
  }
}

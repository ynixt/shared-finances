import { Injectable } from "@angular/core";
import moment, { Moment } from "moment";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { Transaction } from "src/app/@core/models";
import { TransactionType } from "../enums";
import { addHttpParamsIntoUrl, DateUtil } from "../util";
import { HttpClient } from "@angular/common/http";
import { StompService } from "./stomp.service";

@Injectable({
  providedIn: "root"
})
export class TransactionService {
  constructor(private httpClient: HttpClient, private stompService: StompService) {
  }

  newTransaction(transaction: Partial<Transaction>): Observable<Transaction> {
    return this.httpClient.post<Transaction>("/api/transaction", {
      type: transaction.type,
      date: transaction.date,
      value: transaction.value,
      description: transaction.description,
      bankAccountId: transaction.bankAccountId,
      bankAccount2Id: transaction.bankAccount2Id,
      categoryId: transaction.categoryId,
      creditCardId: transaction.creditCardId,
      groupId: transaction.groupId,
      firstUserId: transaction.user?.id,
      secondUserId: transaction.user2?.id,
      creditCardBillDateValue: transaction.creditCardBillDateValue,
      totalInstallments: transaction.totalInstallments
    });
  }

  payCreditCardBill(transaction: Partial<Transaction>): Observable<Transaction> {
    return this.newTransaction({
      ...transaction,
      type: TransactionType.CreditCardBillPayment
    });
  }

  editTransaction(transaction: Transaction): Observable<Transaction> {
    return this.httpClient.put<Transaction>(`/api/transaction/${transaction.id}`, {
      type: transaction.type,
      date: transaction.date,
      value: transaction.value,
      description: transaction.description,
      bankAccountId: transaction.bankAccountId,
      bankAccount2Id: transaction.bankAccount2Id,
      categoryId: transaction.categoryId,
      creditCardId: transaction.creditCardId,
      groupId: transaction.groupId,
      firstUserId: transaction.user?.id,
      secondUserId: transaction.user2?.id,
      creditCardBillDateValue: transaction.creditCardBillDateValue,
      totalInstallments: transaction.totalInstallments
    });
  }

  deleteTransaction(
    transactionId: string,
    groupId: string,
    obj?: { deleteAllInstallments?: boolean; deleteNextInstallments?: boolean }
  ): Observable<void> {
    if (!obj) {
      obj = {};
    }

    const url = addHttpParamsIntoUrl(`/api/transaction/${transactionId}`, {
      groupId,
      ...obj
    });

    return this.httpClient.delete<void>(url);
  }

  isTransactionNegative(transactionType: TransactionType): boolean {
    return [TransactionType.CreditCard, TransactionType.Expense, TransactionType.CreditCardBillPayment].includes(transactionType);
  }

  ifNecessaryMakeValueNegative(value: number, transactionType: TransactionType): number {
    const isTransactionNegative = this.isTransactionNegative(transactionType);

    if ((isTransactionNegative && value > 0) || (!isTransactionNegative && value < 0)) {
      return value * -1;
    }

    return value;
  }

  onTransactionCreated(groupId: string): Observable<string> {
    return this.stompService.watch({
      destination: "/user/queue/group/transaction-created/" + groupId
    }).pipe(
      map(message => JSON.parse(message.body) as Transaction),
      map(transaction => transaction.id)
    );
  }

  onTransactionUpdated(groupId?: string): Observable<string> {
    return this.stompService.watch({
      destination: "/user/queue/group/transaction-updated/" + groupId
    }).pipe(
      map(message => JSON.parse(message.body) as Transaction),
      map(transaction => transaction.id)
    );
  }

  onTransactionDeleted(groupId?: string): Observable<string> {
    return this.stompService.watch({
      destination: "/user/queue/group/transaction-deleted/" + groupId
    }).pipe(
      map(message => JSON.parse(message.body) as Transaction),
      map(transaction => transaction.id)
    );
  }

  /**
   *
   * @param disallowFutureOnSameMonth If true AND 'monthDate' is the same month as the current month, the date that will be returned will be the current date.
   * @returns
   */
  getMaxDate(monthDate: Moment | string, disallowFutureOnSameMonth: boolean): Moment {
    let maxDate = moment(monthDate).endOf("month");

    if (disallowFutureOnSameMonth && moment(monthDate).isSame(moment(), "month") && DateUtil.dateIsBiggerThanToday(maxDate)) {
      maxDate = moment();
    }

    return maxDate;
  }
}

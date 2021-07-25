import { Injectable } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import moment, { Moment } from 'moment';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { Transaction } from 'src/app/@core/models';
import { TransactionType } from '../enums';
import { DateUtil } from '../util';

export const TRANSACTION_CREATED_SUBSCRIPTION = gql`
  subscription transactionCreated($groupId: String) {
    transactionCreated(groupId: $groupId) {
      id
    }
  }
`;

export const TRANSACTION_CREATED_WITH_DATA_SUBSCRIPTION = gql`
  subscription transactionCreated($groupId: String) {
    transactionCreated(groupId: $groupId) {
      id
      transactionType
      group {
        id
        name
      }
      date
      value
      description
      category {
        id
        name
        color
      }
      bankAccountId
      creditCardId
      installment
      totalInstallments
    }
  }
`;

export const TRANSACTION_UPDATED_SUBSCRIPTION = gql`
  subscription transactionUpdated($groupId: String) {
    transactionUpdated(groupId: $groupId) {
      id
    }
  }
`;

export const TRANSACTION_UPDATED_WITH_DATA_SUBSCRIPTION = gql`
  subscription transactionUpdated($groupId: String) {
    transactionUpdated(groupId: $groupId) {
      id
      transactionType
      group {
        id
        name
      }
      date
      value
      description
      category {
        id
        name
        color
      }
      bankAccountId
      creditCardId
      installment
      totalInstallments
    }
  }
`;

export const TRANSACTION_DELETED_SUBSCRIPTION = gql`
  subscription transactionDeleted($groupId: String) {
    transactionDeleted(groupId: $groupId) {
      id
    }
  }
`;

@Injectable({
  providedIn: 'root',
})
export class TransactionService {
  constructor(private apollo: Apollo) {}

  newTransaction(transaction: Partial<Transaction>): Observable<Transaction> {
    return this.apollo
      .mutate<{ newTransaction: Transaction }>({
        mutation: gql`
          mutation(
            $transactionType: String!
            $date: String!
            $value: Float!
            $description: String
            $bankAccountId: String
            $bankAccount2Id: String
            $categoryId: String
            $creditCardId: String
            $groupId: String
            $firstUserId: String!
            $secondUserId: String
            $creditCardBillDate: String
            $totalInstallments: Int
          ) {
            newTransaction(
              transactionType: $transactionType
              date: $date
              value: $value
              description: $description
              bankAccountId: $bankAccountId
              bankAccount2Id: $bankAccount2Id
              categoryId: $categoryId
              creditCardId: $creditCardId
              groupId: $groupId
              firstUserId: $firstUserId
              secondUserId: $secondUserId
              creditCardBillDate: $creditCardBillDate
              totalInstallments: $totalInstallments
            ) {
              id
            }
          }
        `,
        variables: {
          transactionType: transaction.transactionType,
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
          creditCardBillDate: transaction.creditCardBillDate,
          totalInstallments: transaction.totalInstallments,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.newTransaction),
      );
  }

  payCreditCardBill(transaction: Partial<Transaction>): Observable<Transaction> {
    return this.apollo
      .mutate<{ payCreditCardBill: Transaction }>({
        mutation: gql`
          mutation(
            $date: String!
            $value: Float!
            $description: String
            $bankAccountId: String!
            $creditCardId: String!
            $creditCardBillDate: String!
          ) {
            payCreditCardBill(
              date: $date
              value: $value
              description: $description
              bankAccountId: $bankAccountId
              creditCardId: $creditCardId
              creditCardBillDate: $creditCardBillDate
            ) {
              id
            }
          }
        `,
        variables: {
          date: transaction.date,
          value: transaction.value,
          description: transaction.description,
          bankAccountId: transaction.bankAccountId,
          creditCardId: transaction.creditCardId,
          creditCardBillDate: transaction.creditCardBillDate,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.payCreditCardBill),
      );
  }

  editTransaction(transaction: Transaction): Observable<Transaction> {
    return this.apollo
      .mutate<{ editTransaction: Transaction }>({
        mutation: gql`
          mutation(
            $transactionId: String!
            $transactionType: String!
            $date: String!
            $value: Float!
            $description: String
            $bankAccountId: String
            $bankAccount2Id: String
            $categoryId: String
            $creditCardId: String
            $groupId: String
            $firstUserId: String!
            $secondUserId: String
            $creditCardBillDate: String
          ) {
            editTransaction(
              transactionId: $transactionId
              transactionType: $transactionType
              date: $date
              value: $value
              description: $description
              bankAccountId: $bankAccountId
              bankAccount2Id: $bankAccount2Id
              categoryId: $categoryId
              creditCardId: $creditCardId
              groupId: $groupId
              firstUserId: $firstUserId
              secondUserId: $secondUserId
              creditCardBillDate: $creditCardBillDate
            ) {
              id
            }
          }
        `,
        variables: {
          transactionId: transaction.id,
          transactionType: transaction.transactionType,
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
          creditCardBillDate: transaction.creditCardBillDate,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.editTransaction),
      );
  }

  deleteTransaction(
    transactionId: string,
    obj?: { deleteAllInstallments?: boolean; deleteNextInstallments?: boolean },
  ): Observable<boolean> {
    return this.apollo
      .mutate<{ deleteTransaction: boolean }>({
        mutation: gql`
          mutation($transactionId: String!, $deleteAllInstallments: Boolean, $deleteNextInstallments: Boolean) {
            deleteTransaction(
              transactionId: $transactionId
              deleteAllInstallments: $deleteAllInstallments
              deleteNextInstallments: $deleteNextInstallments
            )
          }
        `,
        variables: {
          transactionId,
          deleteAllInstallments: obj?.deleteAllInstallments,
          deleteNextInstallments: obj?.deleteNextInstallments,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.deleteTransaction),
      );
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

  onTransactionCreated(groupId?: string): Observable<string> {
    return this.apollo
      .subscribe<{ transactionCreated: { id: string } }>({
        query: TRANSACTION_CREATED_SUBSCRIPTION,
        variables: {
          groupId,
        },
      })
      .pipe(map(result => result.data.transactionCreated.id));
  }

  onTransactionUpdated(groupId?: string): Observable<string> {
    return this.apollo
      .subscribe<{ transactionUpdated: { id: string } }>({
        query: TRANSACTION_UPDATED_SUBSCRIPTION,
        variables: {
          groupId,
        },
      })
      .pipe(map(result => result.data.transactionUpdated.id));
  }

  onTransactionDeleted(groupId?: string): Observable<string> {
    return this.apollo
      .subscribe<{ transactionDeleted: { id: string } }>({
        query: TRANSACTION_DELETED_SUBSCRIPTION,
        variables: {
          groupId,
        },
      })
      .pipe(map(result => result.data.transactionDeleted.id));
  }

  /**
   *
   * @param disallowFutureOnSameMonth If true AND 'monthDate' is the same month as the current month, the date that will be returned will be the current date.
   * @returns
   */
  getMaxDate(monthDate: Moment | string, disallowFutureOnSameMonth: boolean): Moment {
    let maxDate = moment(monthDate).endOf('month');

    if (disallowFutureOnSameMonth && moment(monthDate).isSame(moment(), 'month') && DateUtil.dateIsBiggerThanToday(maxDate)) {
      maxDate = moment();
    }

    return maxDate;
  }
}

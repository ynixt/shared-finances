import { Injectable } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { Transaction } from 'src/app/@core/models';
import { TransactionType } from '../enums';

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
            ) {
              id
              transactionType
              date
              value
              description
              category {
                id
                name
                color
              }
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
              transactionType
              date
              value
              description
              category {
                id
                name
                color
              }
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
              transactionType
              date
              value
              description
              category {
                id
                name
                color
              }
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

  deleteTransaction(transactionId: string): Observable<boolean> {
    return this.apollo
      .mutate<{ deleteTransaction: boolean }>({
        mutation: gql`
          mutation($transactionId: String!) {
            deleteTransaction(transactionId: $transactionId)
          }
        `,
        variables: {
          transactionId,
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
}

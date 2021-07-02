import { Injectable } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { Transaction } from 'src/app/@core/models';

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
        },
      })
      .pipe(
        take(1),
        map(result => result.data.newTransaction),
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
}

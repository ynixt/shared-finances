import { Injectable } from '@angular/core';
import { Apollo, gql, QueryRef } from 'apollo-angular';
import { EmptyObject } from 'apollo-angular/types';
import { Moment } from 'moment';
import { from, Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { CreditCard, Page, Pagination, Transaction } from 'src/app/@core/models';

const TRANSACTION_OF_CREDIT_CARD_UPDATED_SUBSCRIPTION = gql`
  subscription creditCardTransactionUpdated($creditCardId: String!) {
    creditCardTransactionUpdated(creditCardId: $creditCardId) {
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
      creditCardId
    }
  }
`;

const TRANSACTION_OF_CREDIT_CARD_DELETED_SUBSCRIPTION = gql`
  subscription creditCardTransactionDeleted($creditCardId: String!) {
    creditCardTransactionDeleted(creditCardId: $creditCardId) {
      id
    }
  }
`;

@Injectable({
  providedIn: 'root',
})
export class CreditCardService {
  constructor(private apollo: Apollo) {}

  newCreditCard(creditCard: Partial<CreditCard>): Observable<CreditCard> {
    return this.apollo
      .mutate<{ newCreditCard: CreditCard }>({
        mutation: gql`
          mutation($name: String!, $limit: Float!, $closingDay: Int!, $paymentDay: Int!) {
            newCreditCard(name: $name, limit: $limit, closingDay: $closingDay, paymentDay: $paymentDay) {
              id
              name
              limit
              closingDay
              paymentDay
            }
          }
        `,
        variables: {
          name: creditCard.name,
          limit: creditCard.limit,
          closingDay: creditCard.closingDay,
          paymentDay: creditCard.paymentDay,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.newCreditCard),
      );
  }

  editCreditCard(creditCard: CreditCard): Observable<CreditCard> {
    return this.apollo
      .mutate<{ editCreditCard: CreditCard }>({
        mutation: gql`
          mutation($id: String!, $name: String!, $limit: Float!, $closingDay: Int!, $paymentDay: Int!) {
            editCreditCard(id: $id, name: $name, limit: $limit, closingDay: $closingDay, paymentDay: $paymentDay) {
              id
              name
              limit
              closingDay
              paymentDay
            }
          }
        `,
        variables: {
          id: creditCard.id,
          name: creditCard.name,
          limit: creditCard.limit,
          closingDay: creditCard.closingDay,
          paymentDay: creditCard.paymentDay,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.editCreditCard),
      );
  }

  getById(creditCardId: string): Promise<CreditCard> {
    return this.apollo
      .query<{ creditCard: CreditCard }>({
        query: gql`
          query creditCard($creditCardId: String!) {
            creditCard(creditCardId: $creditCardId) {
              id
              name
              limit
              closingDay
              paymentDay
              billDates
            }
          }
        `,
        variables: {
          creditCardId,
        },
      })
      .pipe(
        map(result => result.data.creditCard),
        take(1),
      )
      .toPromise();
  }

  deleteCreditCard(creditCardId: string): Observable<boolean> {
    return this.apollo
      .mutate<{ deleteCreditCard: boolean }>({
        mutation: gql`
          mutation($creditCardId: String!) {
            deleteCreditCard(creditCardId: $creditCardId)
          }
        `,
        variables: {
          creditCardId,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.deleteCreditCard),
      );
  }

  getTransactions(
    creditCardId: string,
    args?: { maxDate?: Moment; minDate?: Moment },
    pagination?: Pagination,
  ): Observable<Page<Transaction>> {
    const transactionsQueryRef = this.apollo.watchQuery<{ transactions: Page<Transaction> }>({
      query: gql`
        query($creditCardId: String!, $page: Int, $pageSize: Int, $maxDate: String, $minDate: String) {
          transactions(creditCardId: $creditCardId, page: $page, pageSize: $pageSize, maxDate: $maxDate, minDate: $minDate) {
            items {
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
              creditCardId
            }
            total
            page
            pageSize
          }
        }
      `,
      variables: {
        creditCardId,
        page: pagination?.page,
        pageSize: pagination?.pageSize,
        maxDate: args?.maxDate?.toISOString(),
        minDate: args?.minDate?.toISOString(),
      },
    });

    this.subscribeToTransactionChanges(transactionsQueryRef, creditCardId);

    return transactionsQueryRef.valueChanges.pipe(map(result => result.data.transactions));
  }

  private subscribeToTransactionChanges(
    transactionsQueryRef: QueryRef<
      {
        transactions: Page<Transaction>;
      },
      EmptyObject
    >,
    creditCardId: string,
  ) {
    transactionsQueryRef.subscribeToMore({
      document: TRANSACTION_OF_CREDIT_CARD_UPDATED_SUBSCRIPTION,
      variables: {
        creditCardId,
      },
      updateQuery: (prev, { subscriptionData }) => {
        if (prev.transactions != null) {
          const transactionsPage = { items: new Array<Transaction>(), ...prev.transactions };

          transactionsPage.items = JSON.parse(JSON.stringify(transactionsPage.items));

          const transactionUpdatedIndex = transactionsPage.items.findIndex(
            item => item.id === subscriptionData.data.creditCardTransactionUpdated.id,
          );

          if (transactionUpdatedIndex != -1) {
            transactionsPage.items[transactionUpdatedIndex] = subscriptionData.data.creditCardTransactionUpdated;
          }

          prev = {
            transactions: transactionsPage,
          };
          return {
            ...prev,
          };
        }

        return prev;
      },
    });

    transactionsQueryRef.subscribeToMore({
      document: TRANSACTION_OF_CREDIT_CARD_DELETED_SUBSCRIPTION,
      variables: {
        creditCardId,
      },
      updateQuery: (prev, { subscriptionData }) => {
        if (prev.transactions != null) {
          const transactionsPage = { items: new Array<Transaction>(), ...prev.transactions };

          transactionsPage.items = transactionsPage.items.filter(item => item.id !== subscriptionData.data.creditCardTransactionDeleted.id);

          prev = {
            transactions: transactionsPage,
          };
          return {
            ...prev,
          };
        }

        return prev;
      },
    });
  }
}

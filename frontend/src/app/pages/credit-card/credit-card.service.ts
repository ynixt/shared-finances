import { Injectable } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { Moment } from 'moment';
import { from, Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { CreditCard, Page, Pagination, Transaction } from 'src/app/@core/models';

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

    // this.subscribeToTransactionChanges(transactionsQueryRef, bankAccountId);

    return transactionsQueryRef.valueChanges.pipe(map(result => result.data.transactions));
  }
}

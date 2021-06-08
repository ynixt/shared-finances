import { Injectable } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { from, Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { CreditCard } from 'src/app/@core/models';

@Injectable({
  providedIn: 'root',
})
export class CreditCardService {
  constructor(private apollo: Apollo) {}

  newCreditCard(creditCard: CreditCard): Observable<CreditCard> {
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
}

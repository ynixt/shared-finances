import { Injectable } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { take } from 'rxjs/operators';
import { CreditCard } from 'src/app/@core/models';

@Injectable({
  providedIn: 'root',
})
export class CreditCardService {
  constructor(private apollo: Apollo) {}

  async newCreditCard(creditCard: CreditCard): Promise<CreditCard> {
    const result = await this.apollo
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
      .pipe(take(1))
      .toPromise();

    if (result.errors) {
      throw result.errors;
    }

    return result.data.newCreditCard;
  }
}

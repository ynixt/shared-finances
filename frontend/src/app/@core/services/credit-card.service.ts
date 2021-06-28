import { Injectable } from '@angular/core';
import { Apollo, gql, QueryRef } from 'apollo-angular';
import { EmptyObject } from 'apollo-angular/types';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { CreditCard } from '../models';

const CREDIT_CARD_CREATED_SUBSCRIPTION = gql`
  subscription creditCardCreated {
    creditCardCreated {
      id
      name
      closingDay
      paymentDay
      limit
    }
  }
`;

const CREDIT_CARD_UPDATED_SUBSCRIPTION = gql`
  subscription creditCardUpdated {
    creditCardUpdated {
      id
      name
      closingDay
      paymentDay
      limit
    }
  }
`;

const CREDIT_CARD_DELETED_SUBSCRIPTION = gql`
  subscription creditCardDeleted {
    creditCardDeleted {
      id
    }
  }
`;

@Injectable({
  providedIn: 'root',
})
export class CreditCardService {
  private creditCardQueryRef: QueryRef<
    {
      creditCards: CreditCard[];
    },
    EmptyObject
  >;

  constructor(private apollo: Apollo) {}

  watchCreditCards(): Observable<CreditCard[]> {
    this.creditCardQueryRef = this.apollo.watchQuery<{ creditCards: CreditCard[] }>({
      query: gql`
        query creditCards {
          creditCards {
            id
            name
            closingDay
            paymentDay
            limit
          }
        }
      `,
    });

    this.subscribeToChanges();

    return this.creditCardQueryRef.valueChanges.pipe(map(result => result.data.creditCards));
  }

  private subscribeToChanges() {
    this.creditCardQueryRef.subscribeToMore({
      document: CREDIT_CARD_CREATED_SUBSCRIPTION,
      updateQuery: (prev, { subscriptionData }) => {
        const creditCards = prev.creditCards ?? [];

        prev = {
          creditCards: [...creditCards, subscriptionData.data.creditCardCreated],
        };

        return {
          ...prev,
        };
      },
    });

    this.creditCardQueryRef.subscribeToMore({
      document: CREDIT_CARD_UPDATED_SUBSCRIPTION,
      updateQuery: (prev, { subscriptionData }) => {
        const editedCreditCard = subscriptionData.data.creditCardUpdated;

        prev = {
          creditCards: [...prev.creditCards.filter(creditCard => creditCard.id !== editedCreditCard.id), editedCreditCard],
        };

        return {
          ...prev,
        };
      },
    });

    this.creditCardQueryRef.subscribeToMore({
      document: CREDIT_CARD_DELETED_SUBSCRIPTION,
      updateQuery: (prev, { subscriptionData }) => {
        prev = {
          creditCards: prev.creditCards.filter(creditCard => creditCard.id !== subscriptionData.data.creditCardDeleted.id),
        };

        return {
          ...prev,
        };
      },
    });
  }
}

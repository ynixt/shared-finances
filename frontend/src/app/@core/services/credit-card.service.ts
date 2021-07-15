import { Injectable } from '@angular/core';
import { Apollo, gql, QueryRef } from 'apollo-angular';
import { EmptyObject } from 'apollo-angular/types';
import moment, { Moment } from 'moment';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';

import { CreditCard, CreditCardSummary } from '../models';

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

const TRANSACTION_OF_CREDIT_CARD_CREATED_SUBSCRIPTION = gql`
  subscription creditCardTransactionCreated($creditCardId: String!) {
    creditCardTransactionCreated(creditCardId: $creditCardId) {
      id
    }
  }
`;

const TRANSACTION_OF_CREDIT_CARD_UPDATED_SUBSCRIPTION = gql`
  subscription creditCardTransactionUpdated($creditCardId: String!) {
    creditCardTransactionUpdated(creditCardId: $creditCardId) {
      id
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

  watchCreditCards(): Observable<CreditCard[]> {
    const creditCardQueryRef = this.apollo.watchQuery<{ creditCards: CreditCard[] }>({
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

    this.subscribeToChanges(creditCardQueryRef);

    return creditCardQueryRef.valueChanges.pipe(map(result => result.data.creditCards));
  }

  getCreditCardSummary(creditCardId: string, maxCreditCardBillDate: string | Moment): Promise<CreditCardSummary> {
    const creditCardQueryRef = this.apollo.query<{ creditCardSummary: CreditCardSummary }>({
      query: gql`
        query creditCardSummary($creditCardId: String!, $maxCreditCardBillDate: String!) {
          creditCardSummary(creditCardId: $creditCardId, maxCreditCardBillDate: $maxCreditCardBillDate) {
            bill
            expensesOfThisBill
            payments
          }
        }
      `,
      variables: {
        creditCardId,
        maxCreditCardBillDate: moment(maxCreditCardBillDate).utc().toISOString(),
      },
    });

    return creditCardQueryRef
      .pipe(
        take(1),
        map(result => result.data.creditCardSummary),
      )
      .toPromise();
  }

  onTransactionCreated(creditCardId: string): Observable<string> {
    return this.apollo
      .subscribe<{ creditCardTransactionCreated: { id: string } }>({
        query: TRANSACTION_OF_CREDIT_CARD_CREATED_SUBSCRIPTION,
        variables: {
          creditCardId,
        },
      })
      .pipe(map(result => result.data.creditCardTransactionCreated.id));
  }

  onTransactionUpdated(creditCardId: string): Observable<string> {
    return this.apollo
      .subscribe<{ creditCardTransactionUpdated: { id: string } }>({
        query: TRANSACTION_OF_CREDIT_CARD_UPDATED_SUBSCRIPTION,
        variables: {
          creditCardId,
        },
      })
      .pipe(map(result => result.data.creditCardTransactionUpdated.id));
  }

  onTransactionDeleted(creditCardId: string): Observable<string> {
    return this.apollo
      .subscribe<{ creditCardTransactionDeleted: { id: string } }>({
        query: TRANSACTION_OF_CREDIT_CARD_DELETED_SUBSCRIPTION,
        variables: {
          creditCardId,
        },
      })
      .pipe(map(result => result.data.creditCardTransactionDeleted.id));
  }

  private subscribeToChanges(
    creditCardQueryRef: QueryRef<
      {
        creditCards: CreditCard[];
      },
      EmptyObject
    >,
  ) {
    creditCardQueryRef.subscribeToMore({
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

    creditCardQueryRef.subscribeToMore({
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

    creditCardQueryRef.subscribeToMore({
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

  nextBillDate(date: string | Moment, closingDay: number, amountAhead = 1): Moment {
    const dateForThisOption = moment(date).date(closingDay);

    if (dateForThisOption.isSame(date, 'month') === false) {
      // month with less days, like february

      dateForThisOption.subtract(1, 'day');
    }

    return dateForThisOption.add(amountAhead, 'month');
  }

  previousBillDate(date: string | Moment, closingDay: number, amountAhead = 1): Moment {
    const dateForThisOption = moment(date).date(closingDay);

    return dateForThisOption.subtract(amountAhead, 'month');
  }

  findCreditCardBillDate(date: string | Moment, creditCardBillDateOptions: Array<Moment | string>, closingDay: number): Moment | undefined {
    let oneMonthSkipped = false;

    creditCardBillDateOptions = [...creditCardBillDateOptions];

    return creditCardBillDateOptions
      .sort((b1, b2) => {
        const b1Str = typeof b1 === 'string' ? b1 : b1.toISOString();
        const b2Str = typeof b2 === 'string' ? b2 : b2.toISOString();

        return b1Str.localeCompare(b2Str);
      })
      .map(option => moment(option))
      .find(option => {
        const isSameMonth = moment(date).isSame(option, 'month');
        const isSameOrAfterClosingDay = moment(date).date() >= closingDay;

        if (isSameMonth || oneMonthSkipped) {
          if (isSameOrAfterClosingDay && !oneMonthSkipped) {
            oneMonthSkipped = true;
            return false;
          } else {
            return true;
          }
        }

        return false;
      });
  }
}

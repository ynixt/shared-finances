import { Injectable } from '@angular/core';
import { Apollo, gql, QueryRef } from 'apollo-angular';
import { EmptyObject } from 'apollo-angular/types';
import moment, { Moment } from 'moment';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { CHART_DEFAULT_MINIMUM_MONTHS } from '../constants';

import { CreditCard, CreditCardSummary } from '../models';
import { Chart } from '../models/chart';

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
            paymentsOfThisBill
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

  getCreditCardSAvailableLimit(creditCardId: string): Promise<number> {
    const creditCardQueryRef = this.apollo.query<{ creditCardAvailableLimit: number }>({
      query: gql`
        query creditCardAvailableLimit($creditCardId: String!) {
          creditCardAvailableLimit(creditCardId: $creditCardId)
        }
      `,
      variables: {
        creditCardId,
      },
    });

    return creditCardQueryRef
      .pipe(
        take(1),
        map(result => result.data.creditCardAvailableLimit),
      )
      .toPromise();
  }

  getCreditCardBillDates(creditCardId: string): Promise<string[]> {
    const creditCardQueryRef = this.apollo.query<{ creditCardBillDates: string[] }>({
      query: gql`
        query creditCardBillDates($creditCardId: String!) {
          creditCardBillDates(creditCardId: $creditCardId)
        }
      `,
      variables: {
        creditCardId,
      },
    });

    return creditCardQueryRef
      .pipe(
        take(1),
        map(result => result.data.creditCardBillDates),
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

  getTransactionsChart(
    creditCardNamesById: Map<string, string>,
    initialMonthIfNoChart: Moment | string,
    creditCardClosingDay: number,
    args: { creditCardId: string; maxCreditCardBillDate?: Moment; minCreditCardBillDate?: Moment },
    minimumMonths = CHART_DEFAULT_MINIMUM_MONTHS,
  ): Promise<Chart[]> {
    const transactionsChartQueryRef = this.apollo.query<{ transactionsCreditCardChart: Chart[] }>({
      query: gql`
        query($creditCardId: String, $timezone: String!, $maxCreditCardBillDate: String, $minCreditCardBillDate: String) {
          transactionsCreditCardChart(
            creditCardId: $creditCardId
            timezone: $timezone
            maxCreditCardBillDate: $maxCreditCardBillDate
            minCreditCardBillDate: $minCreditCardBillDate
          ) {
            name
            series {
              name
              value
            }
          }
        }
      `,
      variables: {
        creditCardId: args.creditCardId,
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        maxCreditCardBillDate: args?.maxCreditCardBillDate?.toISOString(),
        minCreditCardBillDate: args?.minCreditCardBillDate?.toISOString(),
      },
    });

    return transactionsChartQueryRef
      .pipe(
        map(result => {
          const charts: Chart[] = result.data.transactionsCreditCardChart.map(chart => {
            const dateFormat = 'MM/YYYY';
            const firstDate = chart.series?.length > 0 ? chart.series[0].name : initialMonthIfNoChart;
            const series = chart.series.map(serie => ({
              ...serie,
              name: moment(serie.name).format(dateFormat),
            }));

            if (series.length < minimumMonths) {
              const missing = minimumMonths - series.length;

              for (let i = 0; i < missing; i++) {
                series.splice(i, 0, {
                  name: this.previousBillDate(firstDate, creditCardClosingDay, i + 1).format(dateFormat),
                  value: 0,
                });
              }
            }

            return { name: creditCardNamesById.get(chart.name), series };
          });

          return charts;
        }),
        take(1),
      )
      .toPromise();
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

    if (dateForThisOption.isSame(date, 'month') === false && amountAhead != 0) {
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
    let oneMonthSkipped = creditCardBillDateOptions.length <= 1;

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

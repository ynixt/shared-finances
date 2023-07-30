import { Injectable } from "@angular/core";
import { Apollo, gql, QueryRef } from "apollo-angular";
import { EmptyObject } from "apollo-angular/types";
import moment, { Moment } from "moment";
import { from, lastValueFrom, Observable } from "rxjs";
import { map, take } from "rxjs/operators";
import { DEFAULT_PAGE_SIZE } from "src/app/@core/constants";
import { CreditCard, Page, Pagination, Transaction } from "src/app/@core/models";
import { HttpClient } from "@angular/common/http";
import { addHttpParamsIntoUrl } from "../../@core/util";
import { ISO_DATE_FORMAT } from "../../moment-extension";

const TRANSACTION_OF_CREDIT_CARD_CREATED_SUBSCRIPTION = gql`
  subscription creditCardTransactionCreated($creditCardId: String!) {
    creditCardTransactionCreated(creditCardId: $creditCardId) {
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
  providedIn: "root"
})
export class CreditCardService {
  private apollo: Apollo
  
  constructor(private httpClient: HttpClient) {
  }

  newCreditCard(creditCard: Partial<CreditCard>): Observable<CreditCard> {
    return this.httpClient.post<CreditCard>("/api/credit-card", {
      name: creditCard.name,
      limit: creditCard.limit,
      closingDay: creditCard.closingDay,
      paymentDay: creditCard.paymentDay,
      enabled: creditCard.enabled,
      displayOnGroup: creditCard.displayOnGroup
    });
  }

  editCreditCard(creditCard: CreditCard): Observable<CreditCard> {
    return this.httpClient.put<CreditCard>(`/api/credit-card/${creditCard.id}`, {
      name: creditCard.name,
      limit: creditCard.limit,
      closingDay: creditCard.closingDay,
      paymentDay: creditCard.paymentDay,
      enabled: creditCard.enabled,
      displayOnGroup: creditCard.displayOnGroup
    });
  }

  getById(creditCardId: string): Promise<CreditCard> {
    return lastValueFrom(this.httpClient.get<CreditCard>(`/api/credit-card/${creditCardId}`).pipe(take(1)));
  }

  deleteCreditCard(creditCardId: string): Observable<void> {
    return this.httpClient.delete<void>(`/api/credit-card/${creditCardId}`);
  }

  getTransactions(
    creditCardId: string,
    args?: { maxDate?: Moment; minDate?: Moment; creditCardBillDate: Moment },
    pagination?: Pagination
  ): Observable<Page<Transaction>> {
    const url = addHttpParamsIntoUrl(`/api/credit-card/${creditCardId}/transactions`, {
      page: pagination?.page,
      size: pagination?.size,
      maxDate: args?.maxDate?.format(ISO_DATE_FORMAT),
      minDate: args?.minDate?.format(ISO_DATE_FORMAT),
      creditCardBillDate: args?.creditCardBillDate?.format(ISO_DATE_FORMAT)
    })

    return this.httpClient.get<Page<Transaction>>(url);

    const transactionsQueryRef = this.apollo.watchQuery<{ transactions: Page<Transaction> }>({
      query: gql`
        query($creditCardId: String!, $page: Int, $pageSize: Int, $maxDate: String, $minDate: String, $creditCardBillDate: String) {
          transactions(
            creditCardId: $creditCardId
            page: $page
            pageSize: $pageSize
            maxDate: $maxDate
            minDate: $minDate
            creditCardBillDate: $creditCardBillDate
          ) {
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
              installment
              totalInstallments
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
        size: pagination?.size,
        maxDate: args?.maxDate?.toISOString(),
        minDate: args?.minDate?.toISOString(),
        creditCardBillDate: args?.creditCardBillDate?.toISOString()
      }
    });

    this.subscribeToTransactionChanges(transactionsQueryRef, creditCardId, args.creditCardBillDate);

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
    creditCardBillDate: string | Moment
  ) {
    // transactionsQueryRef.subscribeToMore({
    //   document: TRANSACTION_OF_CREDIT_CARD_CREATED_SUBSCRIPTION,
    //   variables: {
    //     creditCardId,
    //   },
    //   updateQuery: (prev, { subscriptionData }) => {
    //     const newTransaction: Transaction = subscriptionData.data.creditCardTransactionCreated;
    //
    //     if (moment(newTransaction.creditCardBillDate).isSame(creditCardBillDate)) {
    //       if (prev.transactions != null) {
    //         const transactionsPage = { items: new Array<Transaction>(), ...prev.transactions };
    //
    //         transactionsPage.items = [newTransaction, ...JSON.parse(JSON.stringify(transactionsPage.items))];
    //
    //         prev = {
    //           transactions: transactionsPage,
    //         };
    //         return {
    //           ...prev,
    //         };
    //       } else {
    //         const transactionsPage: Page<Transaction> = { items: new Array<Transaction>(), total: 1, page: 1, pageSize: DEFAULT_PAGE_SIZE };
    //
    //         transactionsPage.items = JSON.parse(JSON.stringify(transactionsPage.items));
    //
    //         prev = {
    //           transactions: transactionsPage,
    //         };
    //         return {
    //           ...prev,
    //         };
    //       }
    //     }
    //
    //     return prev;
    //   },
    // });
    //
    // transactionsQueryRef.subscribeToMore({
    //   document: TRANSACTION_OF_CREDIT_CARD_UPDATED_SUBSCRIPTION,
    //   variables: {
    //     creditCardId,
    //   },
    //   updateQuery: (prev, { subscriptionData }) => {
    //     if (prev.transactions != null) {
    //       const transactionsPage = { items: new Array<Transaction>(), ...prev.transactions };
    //
    //       transactionsPage.items = JSON.parse(JSON.stringify(transactionsPage.items));
    //
    //       const transactionUpdatedIndex = transactionsPage.items.findIndex(
    //         item => item.id === subscriptionData.data.creditCardTransactionUpdated.id,
    //       );
    //
    //       if (transactionUpdatedIndex != -1) {
    //         transactionsPage.items[transactionUpdatedIndex] = subscriptionData.data.creditCardTransactionUpdated;
    //       }
    //
    //       prev = {
    //         transactions: transactionsPage,
    //       };
    //       return {
    //         ...prev,
    //       };
    //     }
    //
    //     return prev;
    //   },
    // });
    //
    // transactionsQueryRef.subscribeToMore({
    //   document: TRANSACTION_OF_CREDIT_CARD_DELETED_SUBSCRIPTION,
    //   variables: {
    //     creditCardId,
    //   },
    //   updateQuery: (prev, { subscriptionData }) => {
    //     if (prev.transactions != null) {
    //       const transactionsPage = { items: new Array<Transaction>(), ...prev.transactions };
    //
    //       transactionsPage.items = transactionsPage.items.filter(item => item.id !== subscriptionData.data.creditCardTransactionDeleted.id);
    //
    //       prev = {
    //         transactions: transactionsPage,
    //       };
    //       return {
    //         ...prev,
    //       };
    //     }
    //
    //     return prev;
    //   },
    // });
  }
}

import { Injectable } from '@angular/core';
import { Apollo, gql, QueryRef } from 'apollo-angular';
import { EmptyObject } from 'apollo-angular/types';
import { Moment } from 'moment';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { BankAccount, Page, Pagination, Transaction } from 'src/app/@core/models';

const TRANSACTION_OF_BANK_ACCOUNT_CREATED_SUBSCRIPTION_FOR_BALANCE = gql`
  subscription bankAccountTransactionCreated($bankAccountId: String!) {
    bankAccountTransactionCreated(bankAccountId: $bankAccountId) {
      id
    }
  }
`;

const TRANSACTION_OF_BANK_ACCOUNT_UPDATED_SUBSCRIPTION_FOR_BALANCE = gql`
  subscription bankAccountTransactionUpdated($bankAccountId: String!) {
    bankAccountTransactionUpdated(bankAccountId: $bankAccountId) {
      id
    }
  }
`;

const TRANSACTION_OF_BANK_ACCOUNT_UPDATED_SUBSCRIPTION = gql`
  subscription bankAccountTransactionUpdated($bankAccountId: String!) {
    bankAccountTransactionUpdated(bankAccountId: $bankAccountId) {
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
      bankAccountId
    }
  }
`;

const TRANSACTION_OF_BANK_ACCOUNT_DELETED_SUBSCRIPTION = gql`
  subscription bankAccountTransactionDeleted($bankAccountId: String!) {
    bankAccountTransactionDeleted(bankAccountId: $bankAccountId) {
      id
    }
  }
`;

@Injectable({
  providedIn: 'root',
})
export class BankAccountService {
  constructor(private apollo: Apollo) {}

  getBankAccount(bankAccountId: string): Observable<BankAccount> {
    return this.apollo
      .watchQuery<{ bankAccount: BankAccount }>({
        query: gql`
          query($bankAccountId: String!) {
            bankAccount(bankAccountId: $bankAccountId) {
              id
              name
            }
          }
        `,
        variables: {
          bankAccountId,
        },
      })
      .valueChanges.pipe(map(result => result.data.bankAccount));
  }

  onTransactionCreated(bankAccountId: string): Observable<string> {
    return this.apollo
      .subscribe<{ bankAccountTransactionCreated: { id: string } }>({
        query: TRANSACTION_OF_BANK_ACCOUNT_CREATED_SUBSCRIPTION_FOR_BALANCE,
        variables: {
          bankAccountId,
        },
      })
      .pipe(map(result => result.data.bankAccountTransactionCreated.id));
  }

  onTransactionUpdated(bankAccountId: string): Observable<string> {
    return this.apollo
      .subscribe<{ bankAccountTransactionUpdated: { id: string } }>({
        query: TRANSACTION_OF_BANK_ACCOUNT_UPDATED_SUBSCRIPTION_FOR_BALANCE,
        variables: {
          bankAccountId,
        },
      })
      .pipe(map(result => result.data.bankAccountTransactionUpdated.id));
  }

  onTransactionDeleted(bankAccountId: string): Observable<string> {
    return this.apollo
      .subscribe<{ bankAccountTransactionDeleted: { id: string } }>({
        query: TRANSACTION_OF_BANK_ACCOUNT_DELETED_SUBSCRIPTION,
        variables: {
          bankAccountId,
        },
      })
      .pipe(map(result => result.data.bankAccountTransactionDeleted.id));
  }

  getTransactions(
    bankAccountId: string,
    args?: { maxDate?: Moment; minDate?: Moment },
    pagination?: Pagination,
  ): Observable<Page<Transaction>> {
    const transactionsQueryRef = this.apollo.watchQuery<{ transactions: Page<Transaction> }>({
      query: gql`
        query($bankAccountId: String!, $page: Int, $pageSize: Int, $maxDate: String, $minDate: String) {
          transactions(bankAccountId: $bankAccountId, page: $page, pageSize: $pageSize, maxDate: $maxDate, minDate: $minDate) {
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
              bankAccountId
            }
            total
            page
            pageSize
          }
        }
      `,
      variables: {
        bankAccountId,
        page: pagination?.page,
        pageSize: pagination?.pageSize,
        maxDate: args?.maxDate?.toISOString(),
        minDate: args?.minDate?.toISOString(),
      },
    });

    this.subscribeToTransactionChanges(transactionsQueryRef, bankAccountId);

    return transactionsQueryRef.valueChanges.pipe(map(result => result.data.transactions));
  }

  private subscribeToTransactionChanges(
    transactionsQueryRef: QueryRef<
      {
        transactions: Page<Transaction>;
      },
      EmptyObject
    >,
    bankAccountId: string,
  ) {
    transactionsQueryRef.subscribeToMore({
      document: TRANSACTION_OF_BANK_ACCOUNT_UPDATED_SUBSCRIPTION,
      variables: {
        bankAccountId,
      },
      updateQuery: (prev, { subscriptionData }) => {
        if (prev.transactions != null) {
          const transactionsPage = { items: new Array<Transaction>(), ...prev.transactions };

          transactionsPage.items = JSON.parse(JSON.stringify(transactionsPage.items));

          const transactionUpdatedIndex = transactionsPage.items.findIndex(
            item => item.id === subscriptionData.data.bankAccountTransactionUpdated.id,
          );

          if (transactionUpdatedIndex != -1) {
            transactionsPage.items[transactionUpdatedIndex] = subscriptionData.data.bankAccountTransactionUpdated;
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
      document: TRANSACTION_OF_BANK_ACCOUNT_DELETED_SUBSCRIPTION,
      variables: {
        bankAccountId,
      },
      updateQuery: (prev, { subscriptionData }) => {
        if (prev.transactions != null) {
          const transactionsPage = { items: new Array<Transaction>(), ...prev.transactions };

          transactionsPage.items = transactionsPage.items.filter(
            item => item.id !== subscriptionData.data.bankAccountTransactionDeleted.id,
          );

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

  getBalance(bankAccountId: string, args?: { maxDate?: Moment }, pagination?: Pagination): Promise<number> {
    return this.apollo
      .query<{ bankAccountBalance: number }>({
        query: gql`
          query($bankAccountId: String!, $maxDate: String) {
            bankAccountBalance(bankAccountId: $bankAccountId, maxDate: $maxDate)
          }
        `,
        variables: {
          bankAccountId,
          maxDate: args?.maxDate?.toISOString(),
        },
      })
      .pipe(
        take(1),
        map(result => result.data.bankAccountBalance),
      )
      .toPromise();
  }

  newBankAccount(bankAccount: Partial<BankAccount>): Observable<BankAccount> {
    return this.apollo
      .mutate<{ newBankAccount: BankAccount }>({
        mutation: gql`
          mutation($name: String!) {
            newBankAccount(name: $name) {
              id
              name
              balance
            }
          }
        `,
        variables: {
          name: bankAccount.name,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.newBankAccount),
      );
  }

  changeBankAccountName(bankAccountId: string, name: string): Observable<{ name: string }> {
    return this.apollo
      .mutate<{ changeBankAccountName: { name: string } }>({
        mutation: gql`
          mutation($bankAccountId: String!, $name: String!) {
            changeBankAccountName(bankAccountId: $bankAccountId, name: $name) {
              name
            }
          }
        `,
        variables: {
          bankAccountId,
          name,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.changeBankAccountName),
      );
  }

  deleteBankAccount(bankAccountId: string): Observable<boolean> {
    return this.apollo
      .mutate<{ deleteBankAccount: boolean }>({
        mutation: gql`
          mutation($bankAccountId: String!) {
            deleteBankAccount(bankAccountId: $bankAccountId)
          }
        `,
        variables: {
          bankAccountId,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.deleteBankAccount),
      );
  }
}

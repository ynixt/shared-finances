import { Injectable } from '@angular/core';
import { TranslocoService } from '@ngneat/transloco';
import { Apollo, gql, QueryRef } from 'apollo-angular';
import { EmptyObject } from 'apollo-angular/types';
import moment, { Moment } from 'moment';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { CHART_DEFAULT_MINIMUM_MONTHS, DEFAULT_PAGE_SIZE } from 'src/app/@core/constants';
import { BankAccount, BankAccountSummary, Page, Pagination, Transaction } from 'src/app/@core/models';
import { Chart } from 'src/app/@core/models/chart';

const TRANSACTION_OF_BANK_ACCOUNT_CREATED_SUBSCRIPTION_FOR_BALANCE = gql`
  subscription bankAccountTransactionCreated($bankAccountId: String) {
    bankAccountTransactionCreated(bankAccountId: $bankAccountId) {
      id
    }
  }
`;

const TRANSACTION_OF_BANK_ACCOUNT_CREATED_WITH_DATA_SUBSCRIPTION = gql`
  subscription bankAccountTransactionCreated($bankAccountId: String!) {
    bankAccountTransactionCreated(bankAccountId: $bankAccountId) {
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

const TRANSACTION_OF_BANK_ACCOUNT_UPDATED_SUBSCRIPTION_FOR_BALANCE = gql`
  subscription bankAccountTransactionUpdated($bankAccountId: String) {
    bankAccountTransactionUpdated(bankAccountId: $bankAccountId) {
      id
    }
  }
`;

const TRANSACTION_OF_BANK_ACCOUNT_UPDATED_SUBSCRIPTION = gql`
  subscription bankAccountTransactionUpdated($bankAccountId: String) {
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
  subscription bankAccountTransactionDeleted($bankAccountId: String) {
    bankAccountTransactionDeleted(bankAccountId: $bankAccountId) {
      id
    }
  }
`;

@Injectable({
  providedIn: 'root',
})
export class BankAccountService {
  constructor(private apollo: Apollo, private translocoService: TranslocoService) {}

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

  onTransactionCreated(bankAccountId?: string): Observable<string> {
    return this.apollo
      .subscribe<{ bankAccountTransactionCreated: { id: string } }>({
        query: TRANSACTION_OF_BANK_ACCOUNT_CREATED_SUBSCRIPTION_FOR_BALANCE,
        variables: {
          bankAccountId: bankAccountId,
        },
      })
      .pipe(map(result => result.data.bankAccountTransactionCreated.id));
  }

  onTransactionUpdated(bankAccountId?: string): Observable<string> {
    return this.apollo
      .subscribe<{ bankAccountTransactionUpdated: { id: string } }>({
        query: TRANSACTION_OF_BANK_ACCOUNT_UPDATED_SUBSCRIPTION_FOR_BALANCE,
        variables: {
          bankAccountId: bankAccountId,
        },
      })
      .pipe(map(result => result.data.bankAccountTransactionUpdated.id));
  }

  onTransactionDeleted(bankAccountId?: string): Observable<string> {
    return this.apollo
      .subscribe<{ bankAccountTransactionDeleted: { id: string } }>({
        query: TRANSACTION_OF_BANK_ACCOUNT_DELETED_SUBSCRIPTION,
        variables: {
          bankAccountId: bankAccountId,
        },
      })
      .pipe(map(result => result.data.bankAccountTransactionDeleted.id));
  }

  getTransactions(
    bankAccountId: string,
    args: { maxDate: Moment; minDate: Moment },
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

    this.subscribeToTransactionChanges(transactionsQueryRef, bankAccountId, args.minDate, args.maxDate);

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
    minDate: string | Moment,
    maxDate: string | Moment,
  ) {
    transactionsQueryRef.subscribeToMore({
      document: TRANSACTION_OF_BANK_ACCOUNT_CREATED_WITH_DATA_SUBSCRIPTION,
      variables: {
        bankAccountId,
      },
      updateQuery: (prev, { subscriptionData }) => {
        const newTransaction: Transaction = subscriptionData.data.bankAccountTransactionCreated;

        if (moment(newTransaction.date).isSameOrAfter(minDate) && moment(newTransaction.date).isBefore(maxDate)) {
          if (prev.transactions != null) {
            const transactionsPage = { items: new Array<Transaction>(), ...prev.transactions };

            transactionsPage.items = [newTransaction, ...JSON.parse(JSON.stringify(transactionsPage.items))];

            prev = {
              transactions: transactionsPage,
            };
            return {
              ...prev,
            };
          } else {
            const transactionsPage: Page<Transaction> = { items: new Array<Transaction>(), total: 1, page: 1, pageSize: DEFAULT_PAGE_SIZE };

            transactionsPage.items = JSON.parse(JSON.stringify(transactionsPage.items));

            prev = {
              transactions: transactionsPage,
            };
            return {
              ...prev,
            };
          }
        }

        return prev;
      },
    });

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

  getBankAccountSummary(obj?: { maxDate?: Moment; bankAccountId?: string }): Promise<BankAccountSummary> {
    return this.apollo
      .query<{ bankAccountSummary: BankAccountSummary }>({
        query: gql`
          query($bankAccountId: String, $maxDate: String) {
            bankAccountSummary(bankAccountId: $bankAccountId, maxDate: $maxDate) {
              balance
              expenses
              revenues
            }
          }
        `,
        variables: {
          bankAccountId: obj?.bankAccountId,
          maxDate: obj?.maxDate?.toISOString(),
        },
      })
      .pipe(
        take(1),
        map(result => result.data.bankAccountSummary),
      )
      .toPromise();
  }

  newBankAccount(bankAccount: Partial<BankAccount>): Observable<BankAccount> {
    return this.apollo
      .mutate<{ newBankAccount: BankAccount }>({
        mutation: gql`
          mutation($name: String!, $enabled: Boolean!, $displayOnGroup: Boolean!) {
            newBankAccount(name: $name, enabled: $enabled, displayOnGroup: $displayOnGroup) {
              id
              name
              balance
              enabled
              displayOnGroup
            }
          }
        `,
        variables: {
          name: bankAccount.name,
          enabled: bankAccount.enabled,
          displayOnGroup: bankAccount.displayOnGroup,
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

  getTransactionsChart(
    bankAccountNamesById: Map<string, string>,
    initialMonthIfNoChart: Moment | string,
    args?: { bankAccountId: string; maxDate?: Moment; minDate?: Moment },
    minimumMonths = CHART_DEFAULT_MINIMUM_MONTHS,
  ): Promise<Chart[]> {
    const transactionsChartQueryRef = this.apollo.watchQuery<{ transactionsBankAccountChart: Chart[] }>({
      query: gql`
        query($bankAccountId: String, $timezone: String!, $maxDate: String, $minDate: String) {
          transactionsBankAccountChart(bankAccountId: $bankAccountId, timezone: $timezone, maxDate: $maxDate, minDate: $minDate) {
            name
            series {
              name
              value
            }
          }
        }
      `,
      variables: {
        bankAccountId: args.bankAccountId,
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        maxDate: args?.maxDate?.toISOString(),
        minDate: args?.minDate?.toISOString(),
      },
    });

    return transactionsChartQueryRef.valueChanges
      .pipe(
        map(result => {
          const charts: Chart[] = result.data.transactionsBankAccountChart.map(chart => {
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
                  name: moment(firstDate)
                    .subtract(missing - i, 'month')
                    .format(dateFormat),
                  value: 0,
                });
              }
            }

            let newName = chart.name;

            if (newName.includes('-expenses')) {
              newName = `${bankAccountNamesById.get(newName.replace('-expenses', ''))} (${this.translocoService.translate('expenses')})`;
            } else if (newName.includes('-revenues')) {
              newName = `${bankAccountNamesById.get(newName.replace('-revenues', ''))} (${this.translocoService.translate('revenues')})`;
            } else {
              newName = `${bankAccountNamesById.get(newName)} (${this.translocoService.translate('balance')})`;
            }

            return { name: newName, series };
          });

          return charts;
        }),
        take(1),
      )
      .toPromise();
  }
}

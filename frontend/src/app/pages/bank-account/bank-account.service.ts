import { Injectable } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { Moment } from 'moment';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { BankAccount, Page, Pagination, Transaction } from 'src/app/@core/models';

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

  getTransactions(
    bankAccountId: string,
    args?: { maxDate?: Moment; minDate?: Moment },
    pagination?: Pagination,
  ): Promise<Page<Transaction>> {
    return this.apollo
      .query<{ transactions: Page<Transaction> }>({
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
      })
      .pipe(
        take(1),
        map(result => result.data.transactions),
      )
      .toPromise();
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

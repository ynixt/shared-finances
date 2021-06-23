import { Injectable } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { BankAccount } from 'src/app/@core/models';

@Injectable({
  providedIn: 'root',
})
export class BankAccountService {
  constructor(private apollo: Apollo) {}

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

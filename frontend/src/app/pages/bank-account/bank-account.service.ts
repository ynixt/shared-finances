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
}

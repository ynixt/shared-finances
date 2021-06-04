import { Injectable } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { take } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class InviteService {
  constructor(private apollo: Apollo) {}

  async useInvite(invite: string): Promise<string> {
    const result = await this.apollo
      .mutate<{ useInvite: string }>({
        mutation: gql`
          mutation($invite: String!) {
            useInvite(invite: $invite)
          }
        `,
        variables: {
          invite,
        },
      })
      .pipe(take(1))
      .toPromise();

    if (result.errors) {
      throw result.errors;
    }

    return result.data.useInvite;
  }
}

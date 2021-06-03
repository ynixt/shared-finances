import { Injectable } from '@angular/core';
import { Group } from 'src/app/@core/models/group';
import { Apollo, gql } from 'apollo-angular';
import { take } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class GroupsService {
  constructor(private apollo: Apollo) {}

  async getGroups(): Promise<Group[] | null> {
    try {
      const result = await this.apollo
        .query<{ groups: Group[] }>({
          query: gql`
            query GetGroups {
              groups {
                id
                name
              }
            }
          `,
        })
        .pipe(take(1))
        .toPromise();

      if (result.errors || result.data == null || result.data.groups == null) {
        return [];
      }

      return result.data.groups;
    } catch (err) {
      console.error(err);
      return null;
    }
  }

  async generateShareLink(groupId: string): Promise<string> {
    const result = await this.apollo
      .mutate<{ createShareUrl: string }>({
        mutation: gql`
          mutation($groupId: String!) {
            createShareUrl(groupId: $groupId)
          }
        `,
        variables: {
          groupId,
        },
      })
      .pipe(take(1))
      .toPromise();

    if (result.errors) {
      throw result.errors;
    }

    return result.data.createShareUrl;
  }
}

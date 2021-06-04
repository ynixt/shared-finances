import { Injectable } from '@angular/core';
import { Group } from 'src/app/@core/models/group';
import { Apollo, gql } from 'apollo-angular';
import { catchError, map, take } from 'rxjs/operators';
import { from, Observable, of, Subscription } from 'rxjs';

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

  getGroup(groupId: string): Promise<Group | null> {
    return this.apollo
      .query<{ group: Group }>({
        query: gql`
          query GetGroup($groupId: String!) {
            group(groupId: $groupId) {
              id
              name
            }
          }
        `,
        variables: {
          groupId,
        },
      })
      .pipe(
        map(result => (result.errors || result.data == null || result.data.group == null ? null : result.data.group)),
        take(1),
      )
      .toPromise();
  }

  checkIfGroupChanged(groupId: string): Observable<any> {
    return this.apollo
      .subscribe<{ groupUpdated: Group }>({
        query: gql`
          subscription GetGroup($groupId: String!) {
            groupUpdated(groupId: $groupId) {
              id
              name
            }
          }
        `,
        variables: {
          groupId: groupId,
        },
      })
      .pipe(map(result => (result.errors || result.data == null || result.data.groupUpdated == null ? null : result.data.groupUpdated)));
  }

  async editGroup(group: Group): Promise<Group | null> {
    const result = await this.apollo
      .mutate<{ group: Group }>({
        mutation: gql`
          mutation($id: String!, $name: String!) {
            updateGroup(id: $id, name: $name) {
              id
              name
            }
          }
        `,
        variables: {
          id: group.id,
          name: group.name,
        },
      })
      .pipe(take(1))
      .toPromise();

    if (result.errors) {
      throw result.errors;
    }

    return result.data.group;
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

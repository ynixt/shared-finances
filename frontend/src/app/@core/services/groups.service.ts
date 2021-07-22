import { Injectable } from '@angular/core';
import { Group } from 'src/app/@core/models/group';
import { Apollo, gql } from 'apollo-angular';
import { map, take } from 'rxjs/operators';
import { Observable } from 'rxjs';

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

  deleteGroup(groupId: string): Observable<boolean> {
    return this.apollo
      .mutate<{ deleteGroup: boolean }>({
        mutation: gql`
          mutation ($groupId: String!) {
            deleteGroup(groupId: $groupId)
          }
        `,
        variables: {
          groupId,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.deleteGroup),
      );
  }

  async getGroupsWithUsers(): Promise<Group[] | null> {
    // TODO: It would be better to just search for the user when selecting the group and then search for bank accounts and credit cards in just one query.
    try {
      const result = await this.apollo
        .query<{ groups: Group[] }>({
          query: gql`
            query GetGroups {
              groups {
                id
                name
                users {
                  id
                  name
                  bankAccounts {
                    id
                    name
                  }
                  creditCards {
                    id
                    name
                  }
                }
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

  editGroup(group: Group): Observable<Group | null> {
    return this.apollo
      .mutate<{ group: Group }>({
        mutation: gql`
          mutation ($id: String!, $name: String!) {
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
      .pipe(
        take(1),
        map(result => result.data.group),
      );
  }

  async generateShareLink(groupId: string): Promise<string> {
    const result = await this.apollo
      .mutate<{ createInvite: string }>({
        mutation: gql`
          mutation ($groupId: String!) {
            createInvite(groupId: $groupId)
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

    return result.data.createInvite;
  }
}

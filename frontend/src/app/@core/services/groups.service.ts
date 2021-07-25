import { Injectable } from '@angular/core';
import { Group } from 'src/app/@core/models/group';
import { Apollo, gql, QueryRef } from 'apollo-angular';
import { map, take } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { Moment } from 'moment';
import { GroupSummary, Page, Pagination, Transaction } from '../models';
import { EmptyObject } from 'apollo-angular/types';
import { TRANSACTION_DELETED_SUBSCRIPTION, TRANSACTION_UPDATED_WITH_DATA_SUBSCRIPTION } from './transaction.service';

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
          mutation($groupId: String!) {
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
              users {
                id
                name
              }
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

  getGroupForEdit(groupId: string): Promise<Group | null> {
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

  newGroup(group: Partial<Group>): Observable<Group | null> {
    return this.apollo
      .mutate<{ newGroup: Group }>({
        mutation: gql`
          mutation($name: String!) {
            newGroup(name: $name) {
              id
              name
            }
          }
        `,
        variables: {
          name: group.name,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.newGroup),
      );
  }

  editGroup(group: Group): Observable<Group | null> {
    return this.apollo
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
      .pipe(
        take(1),
        map(result => result.data.group),
      );
  }

  async generateShareLink(groupId: string): Promise<string> {
    const result = await this.apollo
      .mutate<{ createInvite: string }>({
        mutation: gql`
          mutation($groupId: String!) {
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

  getTransactions(groupId: string, args?: { maxDate?: Moment; minDate?: Moment }, pagination?: Pagination): Observable<Page<Transaction>> {
    const transactionsQueryRef = this.apollo.watchQuery<{ transactions: Page<Transaction> }>({
      query: gql`
        query($groupId: String!, $page: Int, $pageSize: Int, $maxDate: String, $minDate: String) {
          transactions(groupId: $groupId, page: $page, pageSize: $pageSize, maxDate: $maxDate, minDate: $minDate) {
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
              creditCardId
              user {
                id
                name
              }
            }
            total
            page
            pageSize
          }
        }
      `,
      variables: {
        groupId,
        page: pagination?.page,
        pageSize: pagination?.pageSize,
        maxDate: args?.maxDate?.toISOString(),
        minDate: args?.minDate?.toISOString(),
      },
    });

    this.subscribeToTransactionChanges(transactionsQueryRef, groupId);

    return transactionsQueryRef.valueChanges.pipe(map(result => result.data.transactions));
  }

  async getGroupSummary(groupId: string, minDate: Moment, maxDate: Moment): Promise<GroupSummary> {
    const summaryQueryRef = this.apollo.query<{ groupSummary: GroupSummary }>({
      query: gql`
        query($groupId: String!, $maxDate: String!, $minDate: String!) {
          groupSummary(groupId: $groupId, maxDate: $maxDate, minDate: $minDate) {
            totalExpenses
            expenses {
              expense
              percentageOfExpenses
              userId
            }
          }
        }
      `,
      variables: {
        groupId,
        maxDate: maxDate?.toISOString(),
        minDate: minDate?.toISOString(),
      },
    });

    return summaryQueryRef
      .pipe(
        map(result => result.data.groupSummary),
        take(1),
      )
      .toPromise();
  }

  private subscribeToTransactionChanges(
    transactionsQueryRef: QueryRef<
      {
        transactions: Page<Transaction>;
      },
      EmptyObject
    >,
    groupId: string,
  ) {
    transactionsQueryRef.subscribeToMore({
      document: TRANSACTION_UPDATED_WITH_DATA_SUBSCRIPTION,
      variables: {
        groupId,
      },
      updateQuery: (prev, { subscriptionData }) => {
        if (prev.transactions != null) {
          const transactionsPage = { items: new Array<Transaction>(), ...prev.transactions };

          transactionsPage.items = JSON.parse(JSON.stringify(transactionsPage.items));

          const transactionUpdatedIndex = transactionsPage.items.findIndex(item => item.id === subscriptionData.data.transactionUpdated.id);

          if (transactionUpdatedIndex != -1) {
            transactionsPage.items[transactionUpdatedIndex] = subscriptionData.data.transactionUpdated;
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
      document: TRANSACTION_DELETED_SUBSCRIPTION,
      variables: {
        groupId,
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
}

import { Injectable } from "@angular/core";
import { Group } from "src/app/@core/models/group";
import { Apollo, gql, QueryRef } from "apollo-angular";
import { map, take } from "rxjs/operators";
import { lastValueFrom, Observable } from "rxjs";
import moment, { Moment } from "moment";
import { CreditCard, GroupSummary, Page, Pagination, Transaction } from "../models";
import { EmptyObject } from "apollo-angular/types";
import {
  TRANSACTION_CREATED_WITH_DATA_SUBSCRIPTION,
  TRANSACTION_DELETED_SUBSCRIPTION,
  TRANSACTION_UPDATED_WITH_DATA_SUBSCRIPTION
} from "./transaction.service";
import { CHART_DEFAULT_MINIMUM_MONTHS, DEFAULT_PAGE_SIZE } from "../constants";
import { Chart } from "../models/chart";
import { HttpClient, HttpParams } from "@angular/common/http";
import { StompService } from "./stomp.service";

@Injectable({
  providedIn: "root"
})
export class GroupsService {
  private apollo: Apollo
  constructor(private stompService: StompService, private httpClient: HttpClient) {
  }

  getGroups(): Observable<Group[]> {
    const w = this.stompService.watch({
      destination: "/user/queue/group"
    });

    this.stompService.publish({ destination: "/app/group" });

    return w.pipe(map(message => JSON.parse(message.body) as Group[]));
  }

  deleteGroup(groupId: string): Observable<void> {
    return this.httpClient.delete<void>(`/api/group/${groupId}`);
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
                    enabled
                    displayOnGroup
                  }
                  creditCards {
                    id
                    name
                    closingDay
                    enabled
                    displayOnGroup
                  }
                }
              }
            }
          `
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

  getGroup(groupId: string): Observable<Group | null> {
    const w = this.stompService.watch({
      destination: `/topic/group/${groupId}`
    });

    this.stompService.publish({ destination: `/app/group/${groupId}` });

    return w.pipe(map(message => JSON.parse(message.body) as Group));
  }

  getGroupForEdit(groupId: string): Promise<Group | null> {
    return lastValueFrom(this.httpClient.get<Group>(`/api/group/${groupId}`).pipe(take(1)));
  }

  newGroup(group: Partial<Group>): Observable<Group | null> {
    return this.httpClient.post<Group>("/api/group", {
      name: group.name
    });
  }

  editGroup(group: Group): Observable<Group | null> {
    return this.httpClient.put<Group>(`/api/group/${group.id}`, {
      name: group.name
    });
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
          groupId
        }
      })
      .pipe(take(1))
      .toPromise();

    if (result.errors) {
      throw result.errors;
    }

    return result.data.createInvite;
  }

  getTransactions(groupId: string, args?: {
    maxDate: Moment;
    minDate: Moment
  }, pagination?: Pagination): Observable<Page<Transaction>> {
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
              installment
              totalInstallments
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
        minDate: args?.minDate?.toISOString()
      }
    });

    this.subscribeToTransactionChanges(transactionsQueryRef, groupId, args.minDate, args.maxDate);

    return transactionsQueryRef.valueChanges.pipe(map(result => result.data.transactions));
  }

  async getGroupSummary(groupId: string, minDate: Moment, maxDate: Moment): Promise<GroupSummary> {
    const httpParams = new HttpParams({
      fromObject: {
        maxDate: maxDate?.toISOString(),
        minDate: minDate?.toISOString()
      }
    });

    return lastValueFrom(this.httpClient.get<GroupSummary>(
      `/api/group/summary/${groupId}?${httpParams.toString()}`).pipe(take(1))
    );
  }

  private subscribeToTransactionChanges(
    transactionsQueryRef: QueryRef<
      {
        transactions: Page<Transaction>;
      },
      EmptyObject
    >,
    groupId: string,
    minDate: Moment,
    maxDate: Moment
  ) {
    // transactionsQueryRef.subscribeToMore({
    //   document: TRANSACTION_CREATED_WITH_DATA_SUBSCRIPTION,
    //   variables: {
    //     groupId,
    //   },
    //   updateQuery: (prev, { subscriptionData }) => {
    //     const newTransaction: Transaction = subscriptionData.data.transactionCreated;
    //
    //     if (moment(newTransaction.date).isSameOrAfter(minDate) && moment(newTransaction.date).isBefore(maxDate)) {
    //       if (prev.transactions != null) {
    //         const transactionsPage = { items: new Array<Transaction>(), ...prev.transactions };
    //
    //         transactionsPage.items = [newTransaction, ...JSON.parse(JSON.stringify(transactionsPage.items))];
    //
    //         prev = {
    //           transactions: transactionsPage,
    //         };
    //         return {
    //           ...prev,
    //         };
    //       } else {
    //         const transactionsPage: Page<Transaction> = { items: new Array<Transaction>(), total: 1, page: 1, pageSize: DEFAULT_PAGE_SIZE };
    //
    //         transactionsPage.items = JSON.parse(JSON.stringify(transactionsPage.items));
    //
    //         prev = {
    //           transactions: transactionsPage,
    //         };
    //         return {
    //           ...prev,
    //         };
    //       }
    //     }
    //
    //     return prev;
    //   },
    // });
    //
    // transactionsQueryRef.subscribeToMore({
    //   document: TRANSACTION_UPDATED_WITH_DATA_SUBSCRIPTION,
    //   variables: {
    //     groupId,
    //   },
    //   updateQuery: (prev, { subscriptionData }) => {
    //     if (prev.transactions != null) {
    //       const transactionsPage = { items: new Array<Transaction>(), ...prev.transactions };
    //
    //       transactionsPage.items = JSON.parse(JSON.stringify(transactionsPage.items));
    //
    //       const transactionUpdatedIndex = transactionsPage.items.findIndex(item => item.id === subscriptionData.data.transactionUpdated.id);
    //
    //       if (transactionUpdatedIndex != -1) {
    //         transactionsPage.items[transactionUpdatedIndex] = subscriptionData.data.transactionUpdated;
    //       }
    //
    //       prev = {
    //         transactions: transactionsPage,
    //       };
    //       return {
    //         ...prev,
    //       };
    //     }
    //
    //     return prev;
    //   },
    // });
    //
    // transactionsQueryRef.subscribeToMore({
    //   document: TRANSACTION_DELETED_SUBSCRIPTION,
    //   variables: {
    //     groupId,
    //   },
    //   updateQuery: (prev, { subscriptionData }) => {
    //     if (prev.transactions != null) {
    //       const transactionsPage = { items: new Array<Transaction>(), ...prev.transactions };
    //
    //       transactionsPage.items = transactionsPage.items.filter(item => item.id !== subscriptionData.data.transactionDeleted.id);
    //
    //       prev = {
    //         transactions: transactionsPage,
    //       };
    //       return {
    //         ...prev,
    //       };
    //     }
    //
    //     return prev;
    //   },
    // });
  }

  getTransactionsChart(
    groupNamesById: Map<string, string>,
    initialMonthIfNoChart: Moment | string,
    args: { groupId: string; maxDate?: Moment; minDate?: Moment },
    minimumMonths = CHART_DEFAULT_MINIMUM_MONTHS
  ): Promise<Chart[]> {
    const transactionsChartQueryRef = this.apollo.query<{ transactionsGroupChart: Chart[] }>({
      query: gql`
        query($groupId: String, $timezone: String!, $maxDate: String, $minDate: String) {
          transactionsGroupChart(groupId: $groupId, timezone: $timezone, maxDate: $maxDate, minDate: $minDate) {
            name
            series {
              name
              value
            }
          }
        }
      `,
      variables: {
        groupId: args.groupId,
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        maxDate: args.maxDate?.toISOString(),
        minDate: args.minDate?.toISOString()
      }
    });

    return transactionsChartQueryRef
      .pipe(
        map(result => {
          const charts: Chart[] = result.data.transactionsGroupChart.map(chart => {
            const dateFormat = "MM/YYYY";
            const firstDate = chart.series?.length > 0 ? chart.series[0].name : initialMonthIfNoChart;
            const series = chart.series.map(serie => ({
              ...serie,
              name: moment(serie.name).format(dateFormat)
            }));

            if (series.length < minimumMonths) {
              const missing = minimumMonths - series.length;

              for (let i = 0; i < missing; i++) {
                series.splice(i, 0, {
                  name: moment(firstDate)
                    .subtract(missing - i, "month")
                    .format(dateFormat),
                  value: 0
                });
              }
            }

            return { name: groupNamesById.get(chart.name), series };
          });

          return charts;
        }),
        take(1)
      )
      .toPromise();
  }
}

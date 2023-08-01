import { Injectable } from "@angular/core";
import { Group } from "src/app/@core/models/group";
import { map, take } from "rxjs/operators";
import { lastValueFrom, Observable } from "rxjs";
import moment, { Moment } from "moment";
import { GroupInvite, GroupSummary, Page, Pagination, Transaction } from "../models";
import { CHART_DEFAULT_MINIMUM_MONTHS } from "../constants";
import { Chart, ChartSerie } from "../models/chart";
import { HttpClient } from "@angular/common/http";
import { StompService } from "./stomp.service";
import { addHttpParamsIntoUrl } from "../util";
import { ISO_DATE_FORMAT } from "../../moment-extension";
import { TransactionValuesAndDateDto } from "../models/transaction-values-and-date";
import { TranslocoService } from "@ngneat/transloco";

@Injectable({
  providedIn: "root"
})
export class GroupsService {
  constructor(private stompService: StompService, private httpClient: HttpClient, private translocoService: TranslocoService) {
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

  async getGroupsWithUsers(): Promise<Group[]> {
    return lastValueFrom(this.httpClient.get<Group[]>("/api/group/with-users").pipe(take(1)));
  }

  getGroup(groupId: string): Observable<Group | null> {
    const w = this.stompService.watch({
      destination: `/user/queue/group/${groupId}`
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

  async generateShareLink(groupId: string): Promise<GroupInvite> {
    return lastValueFrom(
      this.httpClient.post<GroupInvite>(`/api/group/${groupId}/invite`, undefined).pipe(take(1))
    );
  }

  getTransactions(groupId: string, args?: {
    maxDate: Moment;
    minDate: Moment
  }, pagination?: Pagination): Observable<Page<Transaction>> {
    const url = addHttpParamsIntoUrl(`/api/group/${groupId}/transactions`, {
      page: pagination?.page,
      size: pagination?.size,
      maxDate: args?.maxDate?.format(ISO_DATE_FORMAT),
      minDate: args?.minDate?.format(ISO_DATE_FORMAT)
    });

    return this.httpClient.get<Page<Transaction>>(url);
  }

  async getGroupSummary(groupId: string, minDate: Moment, maxDate: Moment): Promise<GroupSummary> {
    const url = addHttpParamsIntoUrl(`/api/group/summary/${groupId}`, {
      maxDate: maxDate?.format(ISO_DATE_FORMAT),
      minDate: minDate?.format(ISO_DATE_FORMAT)
    });

    return lastValueFrom(this.httpClient.get<GroupSummary>(url));
  }

  async getTransactionsChart(
    group: Group,
    initialMonthIfNoChart: Moment | string,
    args: { groupId: string; maxDate?: Moment; minDate?: Moment },
    minimumMonths = CHART_DEFAULT_MINIMUM_MONTHS
  ): Promise<Chart[]> {
    const url = addHttpParamsIntoUrl(`/api/group/${group.id}/chart`, {
      maxDate: args?.maxDate?.format(ISO_DATE_FORMAT),
      minDate: args?.minDate?.format(ISO_DATE_FORMAT)
    });

    const values = await lastValueFrom(
      this.httpClient.get<TransactionValuesAndDateDto[]>(url).pipe(take(1))
    );

    const charts: Chart[] = [];
    const dateFormat = this.translocoService.translate("date-format.month-year");
    const dateFormatFromServer = "YYYY-MM";

    charts.push(new Chart({
      name: `${group.name}`,
      series: values.map(v => new ChartSerie({
        name: moment(v.date, dateFormatFromServer).format(dateFormat),
        value: v.expenses
      }))
    }));

    charts.forEach(chart => {
      if (chart.series.length < minimumMonths) {
        const missing = minimumMonths - chart.series.length;
        const firstDate = chart.series?.length > 0 ? chart.series[0].name : initialMonthIfNoChart;

        for (let i = 0; i < missing; i++) {
          chart.series.splice(i, 0, {
            name: moment(firstDate, dateFormat)
              .subtract(missing - i, "month")
              .format(dateFormat),
            value: 0
          });
        }
      }
    });

    return charts;
  }
}

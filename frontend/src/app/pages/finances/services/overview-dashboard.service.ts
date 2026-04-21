import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import {
  GroupOverviewDashboardDto,
  OverviewDashboardDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/dashboard';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({
  providedIn: 'root',
})
export class OverviewDashboardService {
  constructor(
    private httpClient: HttpClient,
    private userService: UserService,
  ) {}

  async getOverview(month?: string): Promise<OverviewDashboardDto> {
    const user = await this.userService.getUser();

    if (user == null) {
      throw new UserMissingError();
    }

    let params = new HttpParams();
    if (month != null && month.length > 0) {
      params = params.set('month', month);
    }

    return lastValueFrom(
      this.httpClient
        .get<OverviewDashboardDto>('/api/dashboard/overview', {
          params,
        })
        .pipe(take(1)),
    );
  }

  async getGroupOverview(groupId: string, month?: string): Promise<GroupOverviewDashboardDto> {
    const user = await this.userService.getUser();

    if (user == null) {
      throw new UserMissingError();
    }

    let params = new HttpParams();
    if (month != null && month.length > 0) {
      params = params.set('month', month);
    }

    return lastValueFrom(
      this.httpClient
        .get<GroupOverviewDashboardDto>(`/api/groups/${groupId}/dashboard/overview`, {
          params,
        })
        .pipe(take(1)),
    );
  }
}

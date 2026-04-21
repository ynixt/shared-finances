/* eslint-disable */
/* tslint-disable */
import { GroupOverviewDashboardMemberSeriesDto } from './group-overview-dashboard-member-series-dto';
import { OverviewDashboardChartPointDto } from './overview-dashboard-chart-point-dto';

export interface GroupOverviewDashboardSeriesDto {
  byMember: Array<GroupOverviewDashboardMemberSeriesDto>;
  total: Array<OverviewDashboardChartPointDto>;
}

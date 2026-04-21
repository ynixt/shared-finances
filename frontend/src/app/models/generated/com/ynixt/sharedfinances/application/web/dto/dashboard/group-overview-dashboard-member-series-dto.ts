/* eslint-disable */
/* tslint-disable */
import { OverviewDashboardChartPointDto } from './overview-dashboard-chart-point-dto';

export interface GroupOverviewDashboardMemberSeriesDto {
  memberId: string;
  memberName: string;
  points: Array<OverviewDashboardChartPointDto>;
}

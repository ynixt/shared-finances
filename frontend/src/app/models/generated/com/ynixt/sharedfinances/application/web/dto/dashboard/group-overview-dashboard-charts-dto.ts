/* eslint-disable */
/* tslint-disable */
import { GroupOverviewDashboardMemberPieDto } from './group-overview-dashboard-member-pie-dto';
import { GroupOverviewDashboardSeriesDto } from './group-overview-dashboard-series-dto';
import { OverviewDashboardPieSliceDto } from './overview-dashboard-pie-slice-dto';

export interface GroupOverviewDashboardChartsDto {
  cashIn: GroupOverviewDashboardSeriesDto;
  cashInByCategoryByMember: Array<GroupOverviewDashboardMemberPieDto>;
  cashInByCategoryTotal: Array<OverviewDashboardPieSliceDto>;
  expense: GroupOverviewDashboardSeriesDto;
  expenseByCategory: Array<OverviewDashboardPieSliceDto>;
  expenseByCategoryByMember: Array<GroupOverviewDashboardMemberPieDto>;
  expenseByMember: Array<OverviewDashboardPieSliceDto>;
}

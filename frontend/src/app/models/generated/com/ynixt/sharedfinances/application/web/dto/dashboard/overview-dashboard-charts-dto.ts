/* eslint-disable */
/* tslint-disable */
import { OverviewDashboardChartPointDto } from './overview-dashboard-chart-point-dto';
import { OverviewDashboardPieSliceDto } from './overview-dashboard-pie-slice-dto';

export interface OverviewDashboardChartsDto {
  balance: Array<OverviewDashboardChartPointDto>;
  cashIn: Array<OverviewDashboardChartPointDto>;
  cashInByCategory: Array<OverviewDashboardPieSliceDto>;
  cashOut: Array<OverviewDashboardChartPointDto>;
  cashOutByCategory: Array<OverviewDashboardPieSliceDto>;
  expense: Array<OverviewDashboardChartPointDto>;
  expenseByCategory: Array<OverviewDashboardPieSliceDto>;
  expenseByGroup: Array<OverviewDashboardPieSliceDto>;
}

/* eslint-disable */
/* tslint-disable */
import { OverviewDashboardCardDto } from './overview-dashboard-card-dto';
import { OverviewDashboardChartsDto } from './overview-dashboard-charts-dto';

export interface OverviewDashboardDto {
  cards: Array<OverviewDashboardCardDto>;
  charts: OverviewDashboardChartsDto;
  currency: string;
  freeBalanceTotal: number;
  goalCommittedTotal: number;
  goalOverCommittedWarning: boolean;
  selectedMonth: string;
}

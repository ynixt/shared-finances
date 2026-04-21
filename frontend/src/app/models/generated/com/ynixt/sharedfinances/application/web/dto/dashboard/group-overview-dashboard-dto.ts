/* eslint-disable */
/* tslint-disable */
import { GroupOverviewDashboardChartsDto } from './group-overview-dashboard-charts-dto';
import { GroupOverviewDebtPairDto } from './group-overview-debt-pair-dto';
import { OverviewDashboardCardDto } from './overview-dashboard-card-dto';

export interface GroupOverviewDashboardDto {
  cards: Array<OverviewDashboardCardDto>;
  charts: GroupOverviewDashboardChartsDto;
  currency: string;
  debtPairs: Array<GroupOverviewDebtPairDto>;
  goalOverCommittedWarning: boolean;
  selectedMonth: string;
}

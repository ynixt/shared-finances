/* eslint-disable */
/* tslint-disable */

import { OverviewDashboardDetailDto } from './overview-dashboard-detail-dto';

export interface GroupOverviewDebtPairDto {
  currency: string;
  details: Array<OverviewDashboardDetailDto>;
  outstandingAmount: number;
  payerId: string;
  payerName?: string | null;
  receiverId: string;
  receiverName?: string | null;
}

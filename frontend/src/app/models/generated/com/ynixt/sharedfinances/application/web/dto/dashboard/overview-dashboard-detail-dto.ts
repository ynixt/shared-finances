/* eslint-disable */
/* tslint-disable */

export interface OverviewDashboardDetailDto {
  accountOverCommitted: boolean;
  children: Array<OverviewDashboardDetailDto>;
  label: string;
  sourceId?: string | null;
  sourceType: string;
  value: number;
}

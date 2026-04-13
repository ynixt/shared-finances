/* eslint-disable */
/* tslint-disable */

export interface OverviewDashboardDetailDto {
  label: string;
  sourceId?: string | null;
  sourceType: string;
  value: number;
  children?: OverviewDashboardDetailDto[];
  accountOverCommitted?: boolean;
}

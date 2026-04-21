/* eslint-disable */
/* tslint-disable */
import { OverviewDashboardPieSliceDto } from './overview-dashboard-pie-slice-dto';

export interface GroupOverviewDashboardMemberPieDto {
  memberId: string;
  memberName: string;
  slices: Array<OverviewDashboardPieSliceDto>;
}

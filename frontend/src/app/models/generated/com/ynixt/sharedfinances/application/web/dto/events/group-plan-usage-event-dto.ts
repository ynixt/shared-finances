/* eslint-disable */
/* tslint-disable */

import { PlanLimitKey } from '../../../../domain/enums/plan-limit-key';

export interface GroupPlanUsageEventDto {
  groupId: string;
  quota: PlanLimitKey;
  usage: number;
}

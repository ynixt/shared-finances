/* eslint-disable */
/* tslint-disable */

import { PlanLimitKey } from '../../../../domain/enums/plan-limit-key';

export interface PlanUsageEventDto {
  quota: PlanLimitKey;
  usage: number;
}

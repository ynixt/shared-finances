/* eslint-disable */
/* tslint-disable */

import { PlanLimitKey } from '../../../../domain/enums/plan-limit-key';

export interface PlanQuotaEntitlementDto {
  limit?: number | null;
  quota: PlanLimitKey;
  unlimited: boolean;
  usage: number;
  windowEnd?: any | null;
}

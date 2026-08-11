/* eslint-disable */
/* tslint-disable */

import { PlanLimitKey } from '../../../../domain/enums/plan-limit-key';

export interface GroupQuotaEntitlementDto {
  limit?: number | null;
  quota: PlanLimitKey;
  unlimited: boolean;
  usage: number;
}

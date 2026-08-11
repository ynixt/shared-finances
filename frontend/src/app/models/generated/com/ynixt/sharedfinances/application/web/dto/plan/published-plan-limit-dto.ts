/* eslint-disable */
/* tslint-disable */

import { PlanLimitKey } from '../../../../domain/enums/plan-limit-key';

export interface PublishedPlanLimitDto {
  limit?: number | null;
  quota: PlanLimitKey;
  unlimited: boolean;
}

/* eslint-disable */
/* tslint-disable */

import { PlanQuotaEntitlementDto } from './plan-quota-entitlement-dto';
import { UserPlanRole } from '../../../../domain/enums/user-plan-role';

export interface UserEntitlementsDto {
  importMaxLines?: number | null;
  limitsEnabled: boolean;
  projectedDeletionAt?: any | null;
  quotas: Array<PlanQuotaEntitlementDto>;
  role: UserPlanRole;
}

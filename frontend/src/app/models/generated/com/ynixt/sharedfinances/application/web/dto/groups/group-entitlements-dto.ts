/* eslint-disable */
/* tslint-disable */

import { GroupPlanTier } from '../../../../domain/enums/group-plan-tier';
import { GroupQuotaEntitlementDto } from './group-quota-entitlement-dto';

export interface GroupEntitlementsDto {
  limitsEnabled: boolean;
  quotas: Array<GroupQuotaEntitlementDto>;
  tier: GroupPlanTier;
}

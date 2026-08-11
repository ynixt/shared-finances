/* eslint-disable */
/* tslint-disable */

import { GroupPlanTier } from '../../../../domain/enums/group-plan-tier';
import { PublishedPlanLimitDto } from './published-plan-limit-dto';

export interface PublishedGroupTierDto {
  limits: Array<PublishedPlanLimitDto>;
  tier: GroupPlanTier;
}

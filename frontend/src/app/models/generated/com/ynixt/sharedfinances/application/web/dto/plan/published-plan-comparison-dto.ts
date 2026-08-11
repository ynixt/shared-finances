/* eslint-disable */
/* tslint-disable */

import { PublishedGroupTierDto } from './published-group-tier-dto';
import { PublishedUserPlanDto } from './published-user-plan-dto';

export interface PublishedPlanComparisonDto {
  groupTiers: Array<PublishedGroupTierDto>;
  userPlans: Array<PublishedUserPlanDto>;
}

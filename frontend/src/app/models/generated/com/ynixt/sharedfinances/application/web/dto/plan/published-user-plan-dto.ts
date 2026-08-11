/* eslint-disable */
/* tslint-disable */

import { PublishedInactivityPolicyDto } from './published-inactivity-policy-dto';
import { PublishedPlanLimitDto } from './published-plan-limit-dto';
import { UserPlanRole } from '../../../../domain/enums/user-plan-role';

export interface PublishedUserPlanDto {
  inactivityPolicy: PublishedInactivityPolicyDto;
  limits: Array<PublishedPlanLimitDto>;
  plan: UserPlanRole;
}

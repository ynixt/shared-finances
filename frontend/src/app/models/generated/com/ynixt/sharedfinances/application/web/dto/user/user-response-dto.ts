/* eslint-disable */
/* tslint-disable */

import { UserPlanRole } from '../../../../domain/enums/user-plan-role';

export interface UserResponseDto {
  darkMode: boolean;
  defaultCurrency: string;
  email: string;
  emailVerified: boolean;
  firstName: string;
  id: string;
  lang: string;
  lastLoginAt: any;
  lastName: string;
  mfaEnabled: boolean;
  onboardingDone: boolean;
  photoUrl?: string | null;
  role: UserPlanRole;
  tmz: string;
}

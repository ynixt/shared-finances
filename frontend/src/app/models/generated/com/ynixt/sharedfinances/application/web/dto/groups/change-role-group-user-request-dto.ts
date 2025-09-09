/* eslint-disable */
/* tslint-disable */
import { UserGroupRole } from '../../../../domain/enums/user-group-role';

export interface ChangeRoleGroupUserRequestDto {
  memberId: string;
  role: UserGroupRole;
}

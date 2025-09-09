/* eslint-disable */
/* tslint-disable */
import { UserGroupRole } from '../../../../domain/enums/user-group-role';

export interface GroupWithRoleDto {
  id: string;
  name: string;
  role: UserGroupRole;
}

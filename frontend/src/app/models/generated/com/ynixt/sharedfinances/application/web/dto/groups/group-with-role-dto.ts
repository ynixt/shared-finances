/* eslint-disable */
/* tslint-disable */
import { GroupPermissions } from '../../../../domain/enums/group-permissions';
import { UserGroupRole } from '../../../../domain/enums/user-group-role';

export interface GroupWithRoleDto {
  id: string;
  isOwner: boolean;
  name: string;
  ownerUserId: string;
  permissions: Array<GroupPermissions>;
  role: UserGroupRole;
}

/* eslint-disable */
/* tslint-disable */
import { UserGroupRole } from '../../../../domain/enums/user-group-role';
import { UserSimpleDto } from '../user/user-simple-dto';

export interface GroupUserDto {
  role: UserGroupRole;
  user: UserSimpleDto;
}

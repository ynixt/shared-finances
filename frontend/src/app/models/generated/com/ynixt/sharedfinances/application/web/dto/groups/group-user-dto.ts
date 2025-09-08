/* eslint-disable */
/* tslint-disable */
import { UserGroupRole } from '../../../../domain/enums/user-group-role';
import { UserResponseDto } from '../user-response-dto';

export interface GroupUserDto {
  role: UserGroupRole;
  user: UserResponseDto;
}

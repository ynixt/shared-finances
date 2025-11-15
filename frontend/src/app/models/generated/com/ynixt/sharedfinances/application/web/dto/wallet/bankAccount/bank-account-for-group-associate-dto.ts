/* eslint-disable */
/* tslint-disable */
import { UserSimpleDto } from '../../user/user-simple-dto';

export interface BankAccountForGroupAssociateDto {
  currency: string;
  id: string;
  name: string;
  user: UserSimpleDto;
}

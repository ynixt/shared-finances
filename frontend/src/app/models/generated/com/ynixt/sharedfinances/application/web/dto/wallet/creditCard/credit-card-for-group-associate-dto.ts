/* eslint-disable */
/* tslint-disable */
import { UserSimpleDto } from '../../user/user-simple-dto';

export interface CreditCardForGroupAssociateDto {
  currency: string;
  id: string;
  name: string;
  user: UserSimpleDto;
}

/* eslint-disable */
/* tslint-disable */
import { WalletItemType } from '../../../../domain/enums/wallet-item-type';
import { UserSimpleDto } from '../user/user-simple-dto';

export interface WalletItemSearchResponseDto {
  currency: string;
  id: string;
  name: string;
  type: WalletItemType;
  user?: UserSimpleDto | null;
}

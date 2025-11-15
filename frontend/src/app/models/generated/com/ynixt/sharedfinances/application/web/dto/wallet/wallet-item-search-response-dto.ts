/* eslint-disable */
/* tslint-disable */

import { UserSimpleDto } from '../user/user-simple-dto';
import { WalletItemType } from '../../../../domain/enums/wallet-item-type';

export interface WalletItemSearchResponseDto {
  currency: string;
  id: string;
  name: string;
  type: WalletItemType;
  user?: UserSimpleDto | null;
}

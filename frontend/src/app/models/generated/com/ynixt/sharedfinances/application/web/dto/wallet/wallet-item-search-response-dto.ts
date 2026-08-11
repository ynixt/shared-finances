/* eslint-disable */
/* tslint-disable */

import { UserSimpleDto } from '../user/user-simple-dto';
import { WalletItemType } from '../../../../domain/enums/wallet-item-type';

export interface WalletItemSearchResponseDto {
  currency: string;
  daysBetweenDueAndClosing?: number | null;
  dueDay?: number | null;
  dueOnNextBusinessDay?: boolean | null;
  id: string;
  name: string;
  showOnDashboard: boolean;
  type: WalletItemType;
  user?: UserSimpleDto | null;
}

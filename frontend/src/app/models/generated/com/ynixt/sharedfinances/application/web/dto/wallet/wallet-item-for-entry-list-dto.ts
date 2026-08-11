/* eslint-disable */
/* tslint-disable */

import { UserSimpleDto } from '../user/user-simple-dto';
import { WalletItemType } from '../../../../domain/enums/wallet-item-type';

export interface WalletItemForEntryListDto {
  currency: string;
  daysBetweenDueAndClosing?: number | null;
  dueDay?: number | null;
  dueOnNextBusinessDay?: boolean | null;
  enabled: boolean;
  id: string;
  name: string;
  showOnDashboard: boolean;
  type: WalletItemType;
  user?: UserSimpleDto | null;
}

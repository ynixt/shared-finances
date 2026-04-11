/* eslint-disable */
/* tslint-disable */
import { WalletItemType } from '../../../../domain/enums/wallet-item-type';
import { UserSimpleDto } from '../user/user-simple-dto';

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

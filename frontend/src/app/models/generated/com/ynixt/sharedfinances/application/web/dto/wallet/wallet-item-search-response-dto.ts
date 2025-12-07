/* eslint-disable */
/* tslint-disable */
import { WalletItemType } from '../../../../domain/enums/wallet-item-type';
import { UserSimpleDto } from '../user/user-simple-dto';

export interface WalletItemSearchResponseDto {
  currency: string;
  daysBetweenDueAndClosing?: number | null;
  dueDay?: number | null;
  dueOnNextBusinessDay?: boolean | null;
  id: string;
  name: string;
  type: WalletItemType;
  user?: UserSimpleDto | null;
}

/* eslint-disable */
/* tslint-disable */
import { WalletItemForEntryListDto } from '../../wallet/wallet-item-for-entry-list-dto';

export interface EntryResponseDto {
  billDate?: string | null;
  billId?: string | null;
  contributionPercent?: number | null;
  value: number;
  walletItem: WalletItemForEntryListDto;
  walletItemId: string;
}

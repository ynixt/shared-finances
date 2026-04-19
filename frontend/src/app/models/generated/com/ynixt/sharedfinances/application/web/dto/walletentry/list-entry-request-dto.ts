/* eslint-disable */
/* tslint-disable */
import { WalletEntryType } from '../../../../domain/enums/wallet-entry-type';

export interface ListEntryRequestDto {
  bankAccountIds?: Array<string> | null;
  billDate?: string | null;
  billId?: string | null;
  creditCardIds?: Array<string> | null;
  entryTypes?: Array<WalletEntryType> | null;
  groupIds?: Array<string> | null;
  maximumDate?: string | null;
  minimumDate?: string | null;
  pageRequest?: any | null;
  userIds?: Array<string> | null;
  walletItemId?: string | null;
}

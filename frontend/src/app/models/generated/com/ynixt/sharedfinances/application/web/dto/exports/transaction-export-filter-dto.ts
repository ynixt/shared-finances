/* eslint-disable */
/* tslint-disable */

import { WalletEntryType } from '../../../../domain/enums/wallet-entry-type';

export interface TransactionExportFilterDto {
  billDateMode: boolean;
  categoryIds: Array<string>;
  confirmed?: boolean | null;
  dateFrom?: string | null;
  dateTo?: string | null;
  entryTypes: Array<WalletEntryType>;
  groupId?: string | null;
  tags: Array<string>;
  walletItemIds: Array<string>;
}

/* eslint-disable */
/* tslint-disable */

import { EntrySummaryGroupedResultDto } from './entry-summary-grouped-result-dto';
import { WalletItemForEntryListDto } from '../wallet/wallet-item-for-entry-list-dto';

export interface EntrySummaryGroupedDto {
  entries: Array<EntrySummaryGroupedResultDto>;
  walletItem: WalletItemForEntryListDto;
}

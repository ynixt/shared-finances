/* eslint-disable */
/* tslint-disable */
import { WalletItemForEntryListDto } from '../wallet/wallet-item-for-entry-list-dto';
import { EntrySummaryGroupedResultDto } from './entry-summary-grouped-result-dto';

export interface EntrySummaryGroupedDto {
  entries: Array<EntrySummaryGroupedResultDto>;
  walletItem: WalletItemForEntryListDto;
}

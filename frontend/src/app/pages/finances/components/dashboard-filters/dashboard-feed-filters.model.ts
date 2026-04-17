import { WalletEntryType } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums/wallet-entry-type';

export interface DashboardFeedFilters {
  groupIds: string[];
  memberIds: string[];
  creditCardIds: string[];
  bankAccountIds: string[];
  entryTypes: WalletEntryType[];
}

export interface DashboardFilterOption {
  id: string;
  label: string;
}

export const EMPTY_DASHBOARD_FEED_FILTERS: DashboardFeedFilters = {
  groupIds: [],
  memberIds: [],
  creditCardIds: [],
  bankAccountIds: [],
  entryTypes: [],
};

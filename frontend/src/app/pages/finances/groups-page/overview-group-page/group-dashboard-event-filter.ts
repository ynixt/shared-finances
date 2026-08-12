import dayjs from 'dayjs';

import { EventForListDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { DashboardFeedFilters } from '../../components/dashboard-filters/dashboard-feed-filters.model';
import { DateRange } from '../../components/wallet-entry-table/components/advanced-date-picker/advanced-date-picker.component';

export function shouldRefreshGroupDashboardForEvent(
  event: EventForListDto,
  groupId: string | undefined,
  dateRange: DateRange | null | undefined,
  filters: DashboardFeedFilters,
): boolean {
  if (groupId == null || event.group?.id !== groupId || dateRange == null) return false;

  const eventDate = dayjs(event.date);
  const inRange =
    (dateRange.startDate.isBefore(eventDate) || dateRange.startDate.isSame(eventDate, 'day')) &&
    (dateRange.endDate == null || dateRange.endDate.isAfter(eventDate) || dateRange.endDate.isSame(eventDate, 'day'));
  if (!inRange || (filters.entryTypes.length > 0 && !filters.entryTypes.includes(event.type))) return false;

  if (filters.bankAccountIds.length > 0 && !event.entries.some(entry => filters.bankAccountIds.includes(entry.walletItemId))) return false;
  if (filters.creditCardIds.length > 0 && !event.entries.some(entry => filters.creditCardIds.includes(entry.walletItemId))) return false;
  if (
    filters.memberIds.length > 0 &&
    !event.entries.some(entry => entry.walletItem.user?.id != null && filters.memberIds.includes(entry.walletItem.user.id)) &&
    (event.user?.id == null || !filters.memberIds.includes(event.user.id))
  ) {
    return false;
  }

  if (filters.categoryIds.length === 0 && !filters.includeUncategorized) return true;
  const categoryId = event.category?.id;
  if (categoryId == null) return filters.includeUncategorized;
  return filters.categoryIds.length === 0 || filters.categoryIds.includes(categoryId);
}

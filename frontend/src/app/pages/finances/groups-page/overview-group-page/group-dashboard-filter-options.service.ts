import { Injectable, inject } from '@angular/core';

import { WalletItemType } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums/wallet-item-type';
import { DashboardFilterOption } from '../../components/dashboard-filters/dashboard-feed-filters.model';
import { GroupCategoriesService } from '../../services/group-categories.service';
import { GroupWalletItemService } from '../../services/group-wallet-item.service';
import { GroupService } from '../../services/group.service';

@Injectable()
export class GroupDashboardFilterOptionsService {
  private static readonly PAGE_SIZE = 10;

  private readonly groupService = inject(GroupService);
  private readonly groupWalletItemService = inject(GroupWalletItemService);
  private readonly groupCategoriesService = inject(GroupCategoriesService);

  async loadMembers(groupId: string, page = 0, query?: string): Promise<DashboardFilterOption[]> {
    const members = await this.groupService.findAllMembers(groupId);
    const normalizedQuery = query?.trim().toLowerCase();
    const filtered =
      normalizedQuery == null || normalizedQuery.length === 0
        ? members
        : members.filter(member => `${member.user.firstName} ${member.user.lastName}`.toLowerCase().includes(normalizedQuery));
    return this.paginate(filtered, page).map(member => ({
      id: member.user.id,
      label: `${member.user.firstName} ${member.user.lastName}`.trim(),
    }));
  }

  async loadWalletItems(groupId: string, type: WalletItemType, page = 0, query?: string): Promise<DashboardFilterOption[]> {
    const result = await this.groupWalletItemService.getAllItems(
      groupId,
      { page, size: GroupDashboardFilterOptionsService.PAGE_SIZE, sort: 'name' },
      false,
      query,
      type,
    );
    return result.content.map(item => ({ id: item.id, label: item.name }));
  }

  async loadCategories(groupId: string, page = 0, query?: string): Promise<DashboardFilterOption[]> {
    const result = await this.groupCategoriesService.getAllCategories(
      groupId,
      { onlyRoot: false, mountChildren: false, query },
      { page, size: GroupDashboardFilterOptionsService.PAGE_SIZE, sort: 'name' },
    );
    return result.content.map(category => ({ id: category.id, label: category.name }));
  }

  private paginate<T>(items: T[], page: number): T[] {
    const start = page * GroupDashboardFilterOptionsService.PAGE_SIZE;
    return items.slice(start, start + GroupDashboardFilterOptionsService.PAGE_SIZE);
  }
}

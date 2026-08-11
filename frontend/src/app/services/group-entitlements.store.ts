import { Injectable, computed, inject, signal } from '@angular/core';

import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { GroupEntitlementsDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { PlanLimitKey } from '../models/generated/com/ynixt/sharedfinances/domain/enums';
import { GroupService } from '../pages/finances/services/group.service';
import { GroupsActionEventService } from '../pages/finances/services/groups-action-event.service';
import { UserActionEventService } from '../pages/finances/services/user-action-event.service';

@Injectable({ providedIn: 'root' })
export class GroupEntitlementsStore {
  private readonly groupService = inject(GroupService);
  private readonly state = signal<Record<string, GroupEntitlementsDto>>({});

  readonly entitlements = (groupId: string) => computed(() => this.state()[groupId] ?? null);
  readonly limitsEnabled = (groupId: string) => computed(() => this.state()[groupId]?.limitsEnabled === true);
  readonly quota = (groupId: string, key: PlanLimitKey) =>
    computed(() => this.state()[groupId]?.quotas.find(item => item.quota === key) ?? null);

  constructor(events: GroupsActionEventService, userEvents: UserActionEventService) {
    events.planUsage$.pipe(takeUntilDestroyed()).subscribe(event => {
      const current = this.state()[event.data.groupId];
      if (current == null) return;
      this.set(event.data.groupId, {
        ...current,
        quotas: current.quotas.map(item => (item.quota === event.data.quota ? { ...item, usage: event.data.usage } : item)),
      });
    });

    events.ownershipChanged$.pipe(takeUntilDestroyed()).subscribe(event => void this.load(event.data.groupId));
    userEvents.resyncRequired$.pipe(takeUntilDestroyed()).subscribe(() => {
      Object.keys(this.state()).forEach(groupId => void this.load(groupId));
    });
  }

  async load(groupId: string): Promise<void> {
    this.set(groupId, await this.groupService.getEntitlements(groupId));
  }

  private set(groupId: string, value: GroupEntitlementsDto): void {
    this.state.update(current => ({ ...current, [groupId]: value }));
  }
}

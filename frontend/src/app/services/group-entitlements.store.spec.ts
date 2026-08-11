import '@angular/compiler';
import { TestBed } from '@angular/core/testing';

import { Subject } from 'rxjs';

import { describe, expect, it, vi } from 'vitest';

import { GroupService } from '../pages/finances/services/group.service';
import { GroupsActionEventService } from '../pages/finances/services/groups-action-event.service';
import { UserActionEventService } from '../pages/finances/services/user-action-event.service';
import { GroupEntitlementsStore } from './group-entitlements.store';

describe('GroupEntitlementsStore', () => {
  it('loads, applies usage deltas, and refetches after ownership and reconnect events', async () => {
    const usage = new Subject<any>();
    const ownership = new Subject<any>();
    const resync = new Subject<void>();
    const getEntitlements = vi.fn().mockResolvedValue({
      limitsEnabled: true,
      tier: 'COMMON',
      quotas: [{ quota: 'GROUP_GOALS', limit: 10, usage: 1, unlimited: false }],
    });
    TestBed.configureTestingModule({
      providers: [
        { provide: GroupService, useValue: { getEntitlements } },
        { provide: GroupsActionEventService, useValue: { planUsage$: usage, ownershipChanged$: ownership } },
        { provide: UserActionEventService, useValue: { resyncRequired$: resync } },
      ],
    });
    const store = TestBed.inject(GroupEntitlementsStore);
    await store.load('group-1');
    expect(store.quota('group-1', 'GROUP_GOALS')()?.usage).toBe(1);
    expect(store.limitsEnabled('group-1')()).toBe(true);

    usage.next({ data: { groupId: 'group-1', quota: 'GROUP_GOALS', usage: 4 } });
    expect(store.quota('group-1', 'GROUP_GOALS')()?.usage).toBe(4);

    ownership.next({ data: { groupId: 'group-1' } });
    resync.next();
    await Promise.resolve();
    expect(getEntitlements).toHaveBeenCalledTimes(3);
  });
});

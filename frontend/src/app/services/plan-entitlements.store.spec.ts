import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import '@angular/compiler';
import { TestBed } from '@angular/core/testing';

import { Subject } from 'rxjs';

import { describe, expect, it } from 'vitest';

import { PlanUsageEventDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/events';
import { UserResponseDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { UserActionEventService } from '../pages/finances/services/user-action-event.service';
import { PlanEntitlementsStore } from './plan-entitlements.store';
import { UserService } from './user.service';

describe('PlanEntitlementsStore', () => {
  it('loads at sign-in, applies live usage, and refetches after resync', async () => {
    const users = new Subject<UserResponseDto | null | undefined>();
    const usage = new Subject<PlanUsageEventDto>();
    const resync = new Subject<void>();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: UserService, useValue: { user$: users } },
        { provide: UserActionEventService, useValue: { planUsage$: usage, resyncRequired$: resync } },
      ],
    });
    const store = TestBed.inject(PlanEntitlementsStore);
    const http = TestBed.inject(HttpTestingController);

    users.next({ id: 'me' } as UserResponseDto);
    http.expectOne('/api/users/current/entitlements').flush({
      limitsEnabled: true,
      role: 'USER',
      quotas: [{ quota: 'BANK_ACCOUNTS', limit: 10, usage: 1, unlimited: false }],
    });
    await Promise.resolve();
    expect(store.entitlements()?.quotas[0].usage).toBe(1);
    expect(store.limitsEnabled()).toBe(true);

    usage.next({ quota: 'BANK_ACCOUNTS', usage: 2 });
    expect(store.entitlements()?.quotas[0].usage).toBe(2);

    resync.next();
    http.expectOne('/api/users/current/entitlements').flush({
      limitsEnabled: false,
      role: 'USER',
      quotas: [{ quota: 'BANK_ACCOUNTS', limit: 10, usage: 4, unlimited: false }],
    });
    await Promise.resolve();
    expect(store.entitlements()?.quotas[0].usage).toBe(4);
    expect(store.limitsEnabled()).toBe(false);
    http.verify();
  });
});

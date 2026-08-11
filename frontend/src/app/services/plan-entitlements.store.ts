import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, lastValueFrom, take } from 'rxjs';

import { UserEntitlementsDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { PlanLimitKey } from '../models/generated/com/ynixt/sharedfinances/domain/enums';
import { UserActionEventService } from '../pages/finances/services/user-action-event.service';
import { UserService } from './user.service';

@Injectable({ providedIn: 'root' })
export class PlanEntitlementsStore {
  private readonly http = inject(HttpClient);
  private readonly state = signal<UserEntitlementsDto | null>(null);

  readonly entitlements = this.state.asReadonly();
  readonly limitsEnabled = computed(() => this.state()?.limitsEnabled === true);
  readonly quota = (key: PlanLimitKey) => computed(() => this.state()?.quotas.find(item => item.quota === key) ?? null);

  constructor(userService: UserService, events: UserActionEventService) {
    userService.user$.pipe(takeUntilDestroyed()).subscribe(user => {
      if (user === null) this.state.set(null);
      if (user != null) void this.reload();
    });

    events.planUsage$.pipe(takeUntilDestroyed()).subscribe(delta => {
      const current = this.state();
      if (current == null) return;
      this.state.set({
        ...current,
        quotas: current.quotas.map(item => (item.quota === delta.quota ? { ...item, usage: delta.usage } : item)),
      });
    });

    events.resyncRequired$.pipe(takeUntilDestroyed()).subscribe(() => void this.reload());
  }

  async reload(): Promise<void> {
    const value = await lastValueFrom(this.http.get<UserEntitlementsDto>('/api/users/current/entitlements').pipe(take(1)));
    this.state.set(value);
  }
}

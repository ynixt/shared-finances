import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';

import { describe, expect, it, vi } from 'vitest';

import { PlanEntitlementsStore } from '../services/plan-entitlements.store';
import { planLimitsEnabledGuard } from './plan-limits-enabled.guard';

describe('planLimitsEnabledGuard', () => {
  it.each([false, true])('makes limits pages reachable only when limitsEnabled is %s', async enabled => {
    const entitlements = signal(enabled ? ({ limitsEnabled: true } as any) : ({ limitsEnabled: false } as any));
    const limitsEnabled = signal(enabled);
    const reload = vi.fn().mockResolvedValue(undefined);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: PlanEntitlementsStore, useValue: { entitlements, limitsEnabled, reload } }],
    });

    const result = await TestBed.runInInjectionContext(() => (planLimitsEnabledGuard as any)({}, {}));

    if (enabled) {
      expect(result).toBe(true);
    } else {
      expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/app');
    }
  });

  it('loads entitlements before deciding when the store is initially empty', async () => {
    const entitlements = signal<any>(null);
    const limitsEnabled = signal(false);
    const reload = vi.fn(async () => {
      entitlements.set({ limitsEnabled: true });
      limitsEnabled.set(true);
    });
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: PlanEntitlementsStore, useValue: { entitlements, limitsEnabled, reload } }],
    });

    expect(await TestBed.runInInjectionContext(() => (planLimitsEnabledGuard as any)({}, {}))).toBe(true);
    expect(reload).toHaveBeenCalledOnce();
  });
});

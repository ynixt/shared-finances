import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { OpenAuthPreferencesService } from '../services/open-auth-preferences.service';
import { legalDocumentsEnabledGuard, publicPlanComparisonEnabledGuard, registrationEnabledGuard } from './open-auth-feature.guards';

describe('registrationEnabledGuard', () => {
  const preferences = signal({
    emailConfirmationEnabled: true,
    legalDocumentsEnabled: true,
    passwordRecoveryEnabled: true,
    planLimitsEnabled: true,
    registrationEnabled: true,
    turnstileEnabled: true,
  });
  const preferencesService = {
    preferences,
    load: vi.fn().mockResolvedValue(undefined),
  };
  const loginTree = { redirectTo: '/login' };
  const notFoundTree = { redirectTo: '/not-found' };
  const router = {
    createUrlTree: vi.fn().mockImplementation((commands: string[]) => (commands[0] === '/login' ? loginTree : notFoundTree)),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    preferences.update(value => ({ ...value, registrationEnabled: true }));
    preferences.update(value => ({ ...value, legalDocumentsEnabled: true, planLimitsEnabled: true }));
    TestBed.configureTestingModule({
      providers: [
        { provide: OpenAuthPreferencesService, useValue: preferencesService },
        { provide: Router, useValue: router },
      ],
    });
  });

  it.each([
    ['legalDocumentsEnabled', legalDocumentsEnabledGuard],
    ['planLimitsEnabled', publicPlanComparisonEnabledGuard],
  ] as const)('blocks direct public navigation when %s is disabled', async (feature, guard) => {
    preferences.update(value => ({ ...value, [feature]: false }));

    const result = await TestBed.runInInjectionContext(() => guard({} as never, []));

    expect(result).toBe(notFoundTree);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/not-found']);
  });

  it('allows the registration route when enabled', async () => {
    const result = await TestBed.runInInjectionContext(() => registrationEnabledGuard({} as never, []));

    expect(result).toBe(true);
  });

  it('redirects direct registration navigation to login when disabled', async () => {
    preferences.update(value => ({ ...value, registrationEnabled: false }));

    const result = await TestBed.runInInjectionContext(() => registrationEnabledGuard({} as never, []));

    expect(result).toBe(loginTree);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/login']);
  });
});

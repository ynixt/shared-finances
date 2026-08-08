import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { OpenAuthPreferencesService } from '../services/open-auth-preferences.service';
import { registrationEnabledGuard } from './open-auth-feature.guards';

describe('registrationEnabledGuard', () => {
  const preferences = signal({
    emailConfirmationEnabled: true,
    passwordRecoveryEnabled: true,
    registrationEnabled: true,
    turnstileEnabled: true,
  });
  const preferencesService = {
    preferences,
    load: vi.fn().mockResolvedValue(undefined),
  };
  const loginTree = { redirectTo: '/login' };
  const router = {
    createUrlTree: vi.fn().mockReturnValue(loginTree),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    preferences.update(value => ({ ...value, registrationEnabled: true }));
    TestBed.configureTestingModule({
      providers: [
        { provide: OpenAuthPreferencesService, useValue: preferencesService },
        { provide: Router, useValue: router },
      ],
    });
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

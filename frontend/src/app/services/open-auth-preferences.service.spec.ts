import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { OpenAuthPreferencesService } from './open-auth-preferences.service';

describe('OpenAuthPreferencesService', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('publishes disabled registration from backend preferences', async () => {
    const service = TestBed.inject(OpenAuthPreferencesService);
    const load = service.load();
    http.expectOne('/api/open/auth/preferences').flush({
      emailConfirmationEnabled: true,
      legalDocumentsEnabled: true,
      passwordRecoveryEnabled: true,
      planLimitsEnabled: true,
      registrationEnabled: false,
      turnstileEnabled: true,
    });

    await load;

    expect(service.registrationEnabled()).toBe(false);
    expect(service.legalDocumentsEnabled()).toBe(true);
    expect(service.planLimitsEnabled()).toBe(true);
  });

  it('uses the enabled registration default when preferences fail', async () => {
    const service = TestBed.inject(OpenAuthPreferencesService);
    const load = service.load();
    http.expectOne('/api/open/auth/preferences').flush('unavailable', { status: 503, statusText: 'Unavailable' });

    await load;

    expect(service.registrationEnabled()).toBe(true);
    expect(service.legalDocumentsEnabled()).toBe(false);
    expect(service.planLimitsEnabled()).toBe(false);
  });
});

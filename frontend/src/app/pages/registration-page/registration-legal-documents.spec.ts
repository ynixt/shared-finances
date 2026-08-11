import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { EMPTY, Subject, of } from 'rxjs';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CurrencyCatalogService } from '../../components/currency-selector/currency-catalog.service';
import { AuthService } from '../../services/auth.service';
import { BreakpointService } from '../../services/breakpoint.service';
import { ErrorMessageService } from '../../services/error-message.service';
import { LangService } from '../../services/lang.service';
import { OpenAuthPreferencesService } from '../../services/open-auth-preferences.service';
import { PlanEntitlementsStore } from '../../services/plan-entitlements.store';
import { UserService } from '../../services/user.service';
import { RegistrationPageComponent } from './registration-page.component';

describe('RegistrationPageComponent legal documents', () => {
  const legalDocumentsEnabled = signal(false);

  beforeEach(async () => {
    legalDocumentsEnabled.set(false);
    const translate = {
      currentLang: 'en-US',
      onLangChange: new Subject(),
      onFallbackLangChange: EMPTY,
      onTranslationChange: EMPTY,
      onDefaultLangChange: EMPTY,
      instant: (key: string) => key,
      get: (key: string) => of(`translated:${key}`),
      stream: (key: string) => of(key),
      use: (lang: string) => of(lang),
    };

    await TestBed.configureTestingModule({
      imports: [RegistrationPageComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: OpenAuthPreferencesService,
          useValue: {
            legalDocumentsEnabled,
            planLimitsEnabled: signal(false),
            registrationEnabled: signal(true),
            turnstileEnabled: signal(false),
            load: vi.fn().mockResolvedValue(undefined),
          },
        },
        { provide: UserService, useValue: { loading: signal(false), user: signal(null) } },
        { provide: PlanEntitlementsStore, useValue: { limitsEnabled: signal(false) } },
        { provide: BreakpointService, useValue: { isUp: () => signal(false) } },
        { provide: AuthService, useValue: { logout: vi.fn(), submitRegistration: vi.fn() } },
        { provide: ErrorMessageService, useValue: { handleError: vi.fn() } },
        { provide: CurrencyCatalogService, useValue: { getCurrencies: () => of([]) } },
        {
          provide: LangService,
          useValue: {
            currentLang: signal('en-US'),
            getAllLanguages: vi.fn().mockResolvedValue([{ value: 'en-US', name: 'English', current: true }]),
            changeLanguage: vi.fn().mockResolvedValue(undefined),
          },
        },
        { provide: TranslateService, useValue: translate },
      ],
    }).compileComponents();
  });

  it('omits both links and consent requirements while legal documents are absent', async () => {
    const fixture = TestBed.createComponent(RegistrationPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="legal-consent"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/legal/terms"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/legal/privacy"]')).toBeNull();
    expect(fixture.componentInstance.form.get('acceptTerms')?.hasError('required')).toBe(false);
    expect(fixture.componentInstance.form.get('acceptPrivacy')?.hasError('required')).toBe(false);
  });

  it('shows both links and requires both consents while legal documents are presented', async () => {
    legalDocumentsEnabled.set(true);
    const fixture = TestBed.createComponent(RegistrationPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="legal-consent"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/legal/terms"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/legal/privacy"]')).not.toBeNull();
    expect(fixture.componentInstance.form.get('acceptTerms')?.hasError('required')).toBe(true);
    expect(fixture.componentInstance.form.get('acceptPrivacy')?.hasError('required')).toBe(true);
  });
});

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { EMPTY, Subject, of } from 'rxjs';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthService } from '../../services/auth.service';
import { BreakpointService } from '../../services/breakpoint.service';
import { LangService } from '../../services/lang.service';
import { OpenAuthPreferencesService } from '../../services/open-auth-preferences.service';
import { PlanEntitlementsStore } from '../../services/plan-entitlements.store';
import { UserService } from '../../services/user.service';
import { LegalDocumentPageComponent } from './legal-document-page.component';

describe('LegalDocumentPageComponent', () => {
  const planLimitsEnabled = signal(false);
  let legalDoc = 'terms';

  beforeEach(async () => {
    planLimitsEnabled.set(false);
    legalDoc = 'terms';
    await TestBed.configureTestingModule({
      imports: [LegalDocumentPageComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { data: new Proxy({}, { get: () => legalDoc }) } } },
        {
          provide: OpenAuthPreferencesService,
          useValue: { planLimitsEnabled, registrationEnabled: signal(true), load: vi.fn().mockResolvedValue(undefined) },
        },
        { provide: UserService, useValue: { loading: signal(false), user: signal(null) } },
        { provide: PlanEntitlementsStore, useValue: { limitsEnabled: signal(false) } },
        { provide: AuthService, useValue: { logout: vi.fn() } },
        { provide: BreakpointService, useValue: { isUp: () => signal(false) } },
        {
          provide: LangService,
          useValue: { currentLang: signal('en-US'), changeLanguage: vi.fn(), getAllLanguages: vi.fn().mockResolvedValue([]) },
        },
        {
          provide: TranslateService,
          useValue: {
            currentLang: 'en-US',
            onLangChange: new Subject(),
            onFallbackLangChange: EMPTY,
            onTranslationChange: EMPTY,
            onDefaultLangChange: EMPTY,
            instant: (key: string) => key,
            get: (key: string) => of(key),
            use: (lang: string) => of(lang),
          },
        },
      ],
    }).compileComponents();
  });

  it.each(['terms', 'privacy'])('renders plan passages for %s only while limits are enforced', doc => {
    legalDoc = doc;
    const withoutLimits = TestBed.createComponent(LegalDocumentPageComponent);
    withoutLimits.detectChanges();
    expect(withoutLimits.nativeElement.querySelector('a[href="/plans"]')).toBeNull();
    expect(withoutLimits.nativeElement.querySelectorAll('[data-legal-key]')).toHaveLength(doc === 'terms' ? 10 : 10);
    withoutLimits.destroy();

    planLimitsEnabled.set(true);
    const withLimits = TestBed.createComponent(LegalDocumentPageComponent);
    withLimits.detectChanges();
    expect(withLimits.nativeElement.querySelector('a[href="/plans"]')).not.toBeNull();
    expect(withLimits.nativeElement.querySelectorAll('[data-legal-key]')).toHaveLength(doc === 'terms' ? 13 : 12);
  });
});

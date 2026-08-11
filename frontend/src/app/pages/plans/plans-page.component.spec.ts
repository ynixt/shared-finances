import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { EMPTY, Subject, of } from 'rxjs';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthService } from '../../services/auth.service';
import { BreakpointService } from '../../services/breakpoint.service';
import { LangService } from '../../services/lang.service';
import { OpenAuthPreferencesService } from '../../services/open-auth-preferences.service';
import { PlanEntitlementsStore } from '../../services/plan-entitlements.store';
import { UserService } from '../../services/user.service';
import { PlansPageComponent } from './plans-page.component';

describe('PlansPageComponent', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlansPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: OpenAuthPreferencesService,
          useValue: { planLimitsEnabled: signal(true), registrationEnabled: signal(true) },
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
    http = TestBed.inject(HttpTestingController);
  });

  it('renders separate comparisons, unlimited values, and no administrator column', async () => {
    const fixture = TestBed.createComponent(PlansPageComponent);
    fixture.detectChanges();
    http.expectOne('/api/open/plans').flush({
      userPlans: [
        {
          plan: 'USER',
          limits: [{ quota: 'BANK_ACCOUNTS', limit: 10, unlimited: false }],
          inactivityPolicy: { retentionMonths: 12, unlimited: false },
        },
        {
          plan: 'PRO',
          limits: [{ quota: 'BANK_ACCOUNTS', limit: null, unlimited: true }],
          inactivityPolicy: { retentionMonths: null, unlimited: true },
        },
        {
          plan: 'ADMINISTRATOR',
          limits: [{ quota: 'BANK_ACCOUNTS', limit: null, unlimited: true }],
          inactivityPolicy: { retentionMonths: null, unlimited: true },
        },
      ],
      groupTiers: [
        { tier: 'COMMON', limits: [{ quota: 'GROUP_MEMBERS', limit: 4, unlimited: false }] },
        { tier: 'PRO', limits: [{ quota: 'GROUP_MEMBERS', limit: null, unlimited: true }] },
      ],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="user-plan-comparison"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="group-tier-comparison"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-user-plan="ADMINISTRATOR"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-user-plan="PRO"]').textContent).toContain('planIdentity.userPlans.PRO');
    expect(fixture.nativeElement.querySelector('[data-testid="user-plan-comparison"]').textContent).toContain('plans.unlimited');

    const commonAction = fixture.nativeElement.querySelector('[data-testid="plan-action-USER"]');
    expect(commonAction.getAttribute('href')).toBe('/register');
    expect(commonAction.textContent).toContain('plans.actions.startNow');

    const proAction = fixture.nativeElement.querySelector('[data-testid="plan-action-PRO"]');
    expect(proAction.disabled).toBe(true);
    expect(proAction.textContent).toContain('plans.actions.comingSoon');
  });

  it('offers self-hosted with operator-defined limits and links to the GitHub repository', async () => {
    const fixture = TestBed.createComponent(PlansPageComponent);
    fixture.detectChanges();
    http.expectOne('/api/open/plans').flush({
      userPlans: [
        {
          plan: 'USER',
          limits: [
            { quota: 'BANK_ACCOUNTS', limit: 10, unlimited: false },
            { quota: 'CREDIT_CARDS', limit: 2, unlimited: false },
          ],
          inactivityPolicy: { retentionMonths: 12, unlimited: false },
        },
      ],
      groupTiers: [
        {
          tier: 'COMMON',
          limits: [
            { quota: 'GROUP_MEMBERS', limit: 4, unlimited: false },
            { quota: 'GROUP_CATEGORIES', limit: 20, unlimited: false },
          ],
        },
      ],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-user-plan="SELF_HOSTED"]').textContent).toContain('plans.selfHosted.name');
    expect(fixture.nativeElement.querySelector('[data-group-tier="SELF_HOSTED"]').textContent).toContain('plans.selfHosted.name');
    expect(fixture.nativeElement.querySelectorAll('[data-self-hosted-limit]')).toHaveLength(4);
    for (const cell of fixture.nativeElement.querySelectorAll('[data-self-hosted-limit]')) {
      expect(cell.textContent).toContain('plans.selfHosted.youDefine');
    }
    expect(fixture.nativeElement.querySelector('[data-self-hosted-policy]').textContent).toContain('plans.selfHosted.youDefine');
    const selfHostedAction = fixture.nativeElement.querySelector('[data-testid="plan-action-SELF_HOSTED"]');
    expect(selfHostedAction.getAttribute('href')).toBe('https://github.com/ynixt/shared-finances');
    expect(selfHostedAction.textContent).toContain('plans.actions.startNow');
    expect(fixture.nativeElement.querySelector('[data-testid="github-repository-link"]').getAttribute('href')).toBe(
      'https://github.com/ynixt/shared-finances',
    );
  });
});

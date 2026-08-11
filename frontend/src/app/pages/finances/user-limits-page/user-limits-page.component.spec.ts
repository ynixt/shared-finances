import '@angular/compiler';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { describe, expect, it } from 'vitest';

import { UserEntitlementsDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { LocaleService } from '../../../services/locale.service';
import { PlanEntitlementsStore } from '../../../services/plan-entitlements.store';
import { UserService } from '../../../services/user.service';
import { UserLimitsPageComponent } from './user-limits-page.component';

describe('UserLimitsPageComponent', () => {
  it("consolidates every personal quota and converts monthly resets with the user's persisted timezone", async () => {
    const entitlements = signal<UserEntitlementsDto>({
      limitsEnabled: true,
      role: 'PRO',
      quotas: [
        { quota: 'BANK_ACCOUNTS', usage: 3, limit: 10, unlimited: false },
        { quota: 'CREDIT_CARDS', usage: 2, limit: 10, unlimited: false },
        { quota: 'CATEGORIES', usage: 12, limit: 100, unlimited: false },
        { quota: 'GOALS', usage: 4, limit: 20, unlimited: false },
        { quota: 'ACTIVE_SCHEDULES', usage: 6, limit: 50, unlimited: false },
        {
          quota: 'IMPORTS_PER_MONTH',
          usage: 7,
          limit: 30,
          unlimited: false,
          windowEnd: '2026-09-01T00:00:00Z',
        },
        { quota: 'SIMULATIONS_PER_MONTH', usage: 1, limit: null, unlimited: true, windowEnd: '2026-09-01T00:00:00Z' },
        { quota: 'OWNED_GROUPS', usage: 1, limit: 5, unlimited: false },
      ],
    });

    TestBed.configureTestingModule({
      imports: [UserLimitsPageComponent, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        { provide: PlanEntitlementsStore, useValue: { entitlements } },
        { provide: UserService, useValue: { user: signal({ tmz: 'America/Sao_Paulo' }) } },
      ],
    });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('pt-BR', {
      planIdentity: { userPlans: { PRO: 'Pro' } },
      planQuota: {
        quotas: {
          BANK_ACCOUNTS: 'Contas bancárias',
          CREDIT_CARDS: 'Cartões de crédito',
          CATEGORIES: 'Categorias pessoais',
          GOALS: 'Metas pessoais',
          ACTIVE_SCHEDULES: 'Agendamentos ativos',
          IMPORTS_PER_MONTH: 'Importações mensais',
          SIMULATIONS_PER_MONTH: 'Simulações mensais',
          OWNED_GROUPS: 'Grupos próprios',
        },
      },
      financesPage: {
        userLimitsPage: {
          title: 'Meus limites',
          description: 'Acompanhe aqui o consumo dos recursos incluídos no seu plano.',
          used: '{{usage}} usados',
          available: '{{remaining}} disponíveis',
          unlimited: 'Sem limite',
          resets: 'Renova em {{date}}, às {{time}} ({{timezone}})',
        },
      },
    });
    translate.use('pt-BR');
    await TestBed.inject(LocaleService).setLocale('pt-BR');

    const fixture = TestBed.createComponent(UserLimitsPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('[data-quota]')).toHaveLength(8);
    expect(fixture.nativeElement.querySelector('[data-testid="title-badge"]').textContent.trim()).toBe('Pro');
    expect(fixture.nativeElement.querySelector('[data-quota="BANK_ACCOUNTS"]').textContent).toContain('3 / 10');
    expect(fixture.nativeElement.querySelector('[data-quota="BANK_ACCOUNTS"]').textContent).toContain('7 disponíveis');
    expect(fixture.nativeElement.querySelector('[data-quota="IMPORTS_PER_MONTH"]').textContent).toContain(
      'Renova em 31 de agosto de 2026, às 21:00 (America/Sao_Paulo)',
    );
    expect(fixture.nativeElement.querySelector('[data-quota="SIMULATIONS_PER_MONTH"]').textContent).toContain('Sem limite');

    entitlements.update(current => ({
      ...current,
      quotas: current.quotas.map(item => (item.quota === 'BANK_ACCOUNTS' ? { ...item, usage: 6 } : item)),
    }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-quota="BANK_ACCOUNTS"]').textContent).toContain('6 / 10');
    expect(fixture.nativeElement.querySelector('[data-quota="BANK_ACCOUNTS"]').textContent).toContain('4 disponíveis');
  });
});

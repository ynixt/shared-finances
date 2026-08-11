import '@angular/compiler';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { describe, expect, it, vi } from 'vitest';

import { GroupEntitlementsDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { GroupEntitlementsStore } from '../../../../services/group-entitlements.store';
import { GroupService } from '../../services/group.service';
import { GroupLimitsPageComponent } from './group-limits-page.component';

describe('GroupLimitsPageComponent', () => {
  it('consolidates every quota and reacts to live store changes', async () => {
    const entitlements = signal<GroupEntitlementsDto>({
      limitsEnabled: true,
      tier: 'PRO',
      quotas: [
        { quota: 'GROUP_CATEGORIES', usage: 12, limit: 1000, unlimited: false },
        { quota: 'GROUP_GOALS', usage: 3, limit: 10, unlimited: false },
        { quota: 'GROUP_ACTIVE_SCHEDULES', usage: 4, limit: 50, unlimited: false },
        { quota: 'GROUP_MEMBERS', usage: 2, limit: null, unlimited: true },
      ],
    });
    const store = {
      entitlements: vi.fn().mockReturnValue(entitlements),
      load: vi.fn().mockResolvedValue(undefined),
    };

    TestBed.configureTestingModule({
      imports: [GroupLimitsPageComponent, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'group-1' }) } } },
        {
          provide: GroupService,
          useValue: {
            getGroup: vi.fn().mockResolvedValue({
              id: 'group-1',
              name: 'Teste',
              tier: 'PRO',
              ownerUserId: 'owner-1',
              isOwner: false,
              role: 'VIEWER',
              permissions: [],
            }),
          },
        },
        { provide: GroupEntitlementsStore, useValue: store },
      ],
    });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('pt-BR', {
      planIdentity: { groupTiers: { PRO: 'Pro' } },
      planQuota: {
        quotas: {
          GROUP_CATEGORIES: 'Categorias do grupo',
          GROUP_GOALS: 'Metas do grupo',
          GROUP_ACTIVE_SCHEDULES: 'Agendamentos ativos do grupo',
          GROUP_MEMBERS: 'Membros do grupo',
        },
      },
      financesPage: {
        groupsPage: {
          limitsPage: {
            title: 'Limites do grupo: {{name}}',
            description: 'O plano do proprietário define os limites disponíveis para todo o grupo.',
            used: '{{usage}} usados',
            available: '{{remaining}} disponíveis',
            unlimited: 'Sem limite',
          },
        },
      },
    });
    translate.use('pt-BR');

    const fixture = TestBed.createComponent(GroupLimitsPageComponent);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('[data-quota]')).toHaveLength(4);
    expect(fixture.nativeElement.querySelector('[data-testid="title-badge"]').textContent.trim()).toBe('Pro');
    expect(fixture.nativeElement.querySelector('[data-quota="GROUP_GOALS"]').textContent).toContain('3 / 10');
    expect(fixture.nativeElement.querySelector('[data-quota="GROUP_GOALS"]').textContent).toContain('7 disponíveis');
    expect(fixture.nativeElement.querySelector('[data-quota="GROUP_MEMBERS"]').textContent).toContain('Sem limite');

    entitlements.update(current => ({
      ...current,
      quotas: current.quotas.map(item => (item.quota === 'GROUP_GOALS' ? { ...item, usage: 6 } : item)),
    }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-quota="GROUP_GOALS"]').textContent).toContain('6 / 10');
    expect(fixture.nativeElement.querySelector('[data-quota="GROUP_GOALS"]').textContent).toContain('4 disponíveis');
    expect(store.load).toHaveBeenCalledWith('group-1');
  });
});

import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import { BrowserDynamicTestingModule, platformBrowserDynamicTesting } from '@angular/platform-browser-dynamic/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ErrorMessageService } from '../../../../services/error-message.service';
import { GroupDebtService } from '../../services/group-debt.service';
import { GroupService } from '../../services/group.service';
import { GroupDebtsPageComponent } from './group-debts-page.component';

const testEnv = globalThis as typeof globalThis & { __angularTestEnvInit?: boolean };
if (testEnv.__angularTestEnvInit !== true) {
  getTestBed().initTestEnvironment(BrowserDynamicTestingModule, platformBrowserDynamicTesting());
  testEnv.__angularTestEnvInit = true;
}

describe('GroupDebtsPageComponent', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  async function setup(params?: {
    dateQuery?: string;
    members?: Array<{ user: { id: string; firstName: string; lastName: string } }>;
    pairHistory?: any[];
  }) {
    const groupServiceMock = {
      getGroup: vi.fn().mockResolvedValue({ id: 'group-1', name: 'Group', permissions: ['SEND_ENTRIES'] }),
      findAllMembers: vi
        .fn()
        .mockResolvedValue(
          params?.members ?? [
            { user: { id: 'a', firstName: 'Ana', lastName: 'Silva' } },
            { user: { id: 'b', firstName: 'Bruno', lastName: 'Souza' } },
            { user: { id: 'c', firstName: 'Carla', lastName: 'Melo' } },
          ],
        ),
    };
    const groupDebtServiceMock = {
      getWorkspace: vi.fn().mockResolvedValue({ balances: [] }),
      listHistory: vi.fn().mockResolvedValue([]),
      listPairHistory: vi.fn().mockResolvedValue(params?.pairHistory ?? []),
    };
    const routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
    };
    const errorMessageServiceMock = {
      handleError: vi.fn(),
    };
    const translateServiceMock = {
      instant: vi.fn((key: string, values?: Record<string, string>) => {
        if (values?.['id'] != null) {
          return `${key}:${values['id']}`;
        }

        return key;
      }),
    };

    await TestBed.configureTestingModule({
      imports: [GroupDebtsPageComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: 'group-1' }),
              queryParamMap: convertToParamMap(params?.dateQuery == null ? {} : { date: params.dateQuery }),
            },
          },
        },
        { provide: Router, useValue: routerMock },
        { provide: GroupService, useValue: groupServiceMock },
        { provide: GroupDebtService, useValue: groupDebtServiceMock },
        { provide: MessageService, useValue: {} },
        { provide: ErrorMessageService, useValue: errorMessageServiceMock },
        { provide: TranslateService, useValue: translateServiceMock },
      ],
    })
      .overrideComponent(GroupDebtsPageComponent, {
        set: { template: '' },
      })
      .compileComponents();

    const fixture = TestBed.createComponent(GroupDebtsPageComponent);
    await fixture.whenStable();

    return {
      component: fixture.componentInstance,
      groupDebtServiceMock,
      routerMock,
    };
  }

  it('loads current month workspace and pair history by default', async () => {
    const { component, groupDebtServiceMock } = await setup();
    const expectedMonth = dayjs(component.selectedMonth()).format('YYYY-MM');

    expect(groupDebtServiceMock.getWorkspace).toHaveBeenCalledWith('group-1', expectedMonth);
    expect(groupDebtServiceMock.listPairHistory).toHaveBeenCalledWith('group-1', { selectedMonth: expectedMonth });
    expect(expectedMonth).toBe(dayjs().format('YYYY-MM'));
  });

  it('reloads workspace and pair history when user changes month', async () => {
    const { component, groupDebtServiceMock, routerMock } = await setup({ dateQuery: '2026-04' });

    expect(groupDebtServiceMock.getWorkspace).toHaveBeenCalledWith('group-1', '2026-04');
    expect(groupDebtServiceMock.listPairHistory).toHaveBeenCalledWith('group-1', { selectedMonth: '2026-04' });

    const nextRange = {
      startDate: dayjs('2026-06-01'),
      endDate: dayjs('2026-06-01'),
      sameMonth: true,
    };

    component.dateControl.setValue(nextRange, { emitEvent: false });
    await component.onSelectedMonthChange(nextRange, true);

    expect(routerMock.navigate).toHaveBeenCalledWith(
      [],
      expect.objectContaining({
        queryParams: { date: '2026-06', startDate: null, endDate: null },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      }),
    );
    expect(groupDebtServiceMock.getWorkspace).toHaveBeenLastCalledWith('group-1', '2026-06');
    expect(groupDebtServiceMock.listPairHistory).toHaveBeenLastCalledWith('group-1', { selectedMonth: '2026-06' });
  });

  it('sorts pair blocks alphabetically and lines by transaction date desc then name asc', async () => {
    const { component } = await setup({
      pairHistory: [
        {
          firstUserId: 'c',
          secondUserId: 'a',
          currency: 'BRL',
          month: '2026-06',
          netPayerId: 'c',
          netReceiverId: 'a',
          netAmount: 10,
          chargeDelta: 10,
          settlementDelta: 0,
          manualAdjustmentDelta: 0,
          lines: [
            {
              id: 'line-2',
              payerId: 'c',
              receiverId: 'a',
              month: '2026-06',
              transactionDate: '2026-06-10',
              currency: 'BRL',
              deltaSigned: 10,
              reasonKind: 'BENEFICIARY_CHARGE',
              createdByUserId: 'c',
              carriedOver: false,
              projected: false,
              note: 'Beta',
              sourceWalletEventId: null,
              sourceWalletEvent: null,
              sourceMovementId: null,
              createdAt: null,
            },
            {
              id: 'line-1',
              payerId: 'c',
              receiverId: 'a',
              month: '2026-06',
              transactionDate: '2026-06-10',
              currency: 'BRL',
              deltaSigned: 5,
              reasonKind: 'BENEFICIARY_CHARGE',
              createdByUserId: 'c',
              carriedOver: false,
              projected: false,
              note: 'Alpha',
              sourceWalletEventId: null,
              sourceWalletEvent: null,
              sourceMovementId: null,
              createdAt: null,
            },
          ],
        },
        {
          firstUserId: 'b',
          secondUserId: 'a',
          currency: 'BRL',
          month: '2026-06',
          netPayerId: 'b',
          netReceiverId: 'a',
          netAmount: 15,
          chargeDelta: 15,
          settlementDelta: 0,
          manualAdjustmentDelta: 0,
          lines: [
            {
              id: 'line-3',
              payerId: 'b',
              receiverId: 'a',
              month: '2026-06',
              transactionDate: '2026-06-11',
              currency: 'BRL',
              deltaSigned: 15,
              reasonKind: 'BENEFICIARY_CHARGE',
              createdByUserId: 'b',
              carriedOver: false,
              projected: false,
              note: null,
              sourceWalletEventId: null,
              sourceWalletEvent: { date: '2026-06-11', name: 'Compra', recurrenceConfig: null, installment: null },
              sourceMovementId: null,
              createdAt: null,
            },
          ],
        },
      ],
    });

    const items = component.pairHistoryGridItems();

    expect(items.map(item => item.pairLabel)).toEqual(['Ana Silva / Bruno Souza', 'Ana Silva / Carla Melo']);
    expect(items[1].lines.map(line => line.displayName)).toEqual(['Alpha', 'Beta']);
  });

  it('opens settle value page with absolute amount from the line', async () => {
    const { component, routerMock } = await setup();

    component.openSettleValuePage({
      id: 'movement-1',
      payerId: 'payer-1',
      receiverId: 'receiver-1',
      month: '2026-06',
      transactionDate: '2026-06-10',
      currency: 'BRL',
      deltaSigned: -12.5,
      reasonKind: 'MANUAL_ADJUSTMENT',
      createdByUserId: 'payer-1',
      carriedOver: false,
      projected: false,
      note: null,
      sourceWalletEventId: null,
      sourceWalletEvent: null,
      sourceMovementId: null,
      createdAt: null,
    });

    expect(routerMock.navigate).toHaveBeenCalledWith(['/app/groups', 'group-1', 'debts', 'settlements', 'new'], {
      queryParams: {
        payerId: 'payer-1',
        receiverId: 'receiver-1',
        amount: 12.5,
      },
    });
  });

  it('labels carried over lines as prior-month open balance', async () => {
    const { component } = await setup({ dateQuery: '2026-06' });

    expect(
      component.sourceReferenceLabel({
        id: 'movement-1',
        payerId: 'payer-1',
        receiverId: 'receiver-1',
        month: '2026-05',
        transactionDate: null,
        currency: 'BRL',
        deltaSigned: 538.86,
        reasonKind: 'BENEFICIARY_CHARGE',
        createdByUserId: 'receiver-1',
        carriedOver: true,
        projected: false,
        note: null,
        sourceWalletEventId: null,
        sourceWalletEvent: null,
        sourceMovementId: null,
        createdAt: null,
      }),
    ).toBe('financesPage.groupsPage.debtsPage.carryoverSource');
  });
});

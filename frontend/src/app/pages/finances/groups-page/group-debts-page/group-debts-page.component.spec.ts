import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ErrorMessageService } from '../../../../services/error-message.service';
import { GroupDebtService } from '../../services/group-debt.service';
import { GroupService } from '../../services/group.service';
import { GroupDebtsPageComponent } from './group-debts-page.component';

describe('GroupDebtsPageComponent', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  async function setup(dateQuery?: string) {
    const groupServiceMock = {
      getGroup: vi.fn().mockResolvedValue({ id: 'group-1', name: 'Group', permissions: ['SEND_ENTRIES'] }),
      findAllMembers: vi.fn().mockResolvedValue([]),
    };
    const groupDebtServiceMock = {
      getWorkspace: vi.fn().mockResolvedValue({ balances: [] }),
      listHistory: vi.fn().mockResolvedValue([]),
      getMonthlyDrilldown: vi
        .fn()
        .mockResolvedValue({
          month: '2026-06',
          payerId: 'payer-1',
          receiverId: 'receiver-1',
          currency: 'BRL',
          netAmount: 0,
          chargeDelta: 0,
          settlementDelta: 0,
          manualAdjustmentDelta: 0,
          lines: [],
        }),
    };
    const routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
    };
    const errorMessageServiceMock = {
      handleError: vi.fn(),
    };
    const translateServiceMock = {
      instant: vi.fn((key: string) => key),
    };

    await TestBed.configureTestingModule({
      imports: [GroupDebtsPageComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: 'group-1' }),
              queryParamMap: convertToParamMap(dateQuery == null ? {} : { date: dateQuery }),
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

  it('loads current month workspace and history by default', async () => {
    const { component, groupDebtServiceMock } = await setup();
    const expectedMonth = dayjs(component.selectedMonth()).format('YYYY-MM');

    expect(groupDebtServiceMock.getWorkspace).toHaveBeenCalledWith('group-1', expectedMonth);
    expect(groupDebtServiceMock.listHistory).toHaveBeenCalledWith('group-1', { selectedMonth: expectedMonth });
    expect(expectedMonth).toBe(dayjs().format('YYYY-MM'));
  });

  it('reloads workspace and history when user changes month', async () => {
    const { component, groupDebtServiceMock, routerMock } = await setup('04-2026');

    expect(groupDebtServiceMock.getWorkspace).toHaveBeenCalledWith('group-1', '2026-04');
    expect(groupDebtServiceMock.listHistory).toHaveBeenCalledWith('group-1', { selectedMonth: '2026-04' });

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
        queryParams: { date: '06-2026', startDate: null, endDate: null },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      }),
    );
    expect(groupDebtServiceMock.getWorkspace).toHaveBeenLastCalledWith('group-1', '2026-06');
    expect(groupDebtServiceMock.listHistory).toHaveBeenLastCalledWith('group-1', { selectedMonth: '2026-06' });
  });

  it('opens monthly drilldown with the selected month', async () => {
    const { component, groupDebtServiceMock } = await setup('06-2026');

    await component.openMonthlyDrilldown({
      payerId: 'payer-1',
      receiverId: 'receiver-1',
      currency: 'BRL',
      outstandingAmount: 10,
      monthlyComposition: [],
    });

    expect(groupDebtServiceMock.getMonthlyDrilldown).toHaveBeenCalledWith('group-1', {
      payerId: 'payer-1',
      receiverId: 'receiver-1',
      currency: 'BRL',
      selectedMonth: '2026-06',
    });
  });

  it('labels carried over lines as prior-month open balance', async () => {
    const { component } = await setup('06-2026');

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

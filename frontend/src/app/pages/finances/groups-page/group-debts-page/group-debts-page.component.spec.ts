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

  async function setup(monthQuery?: string) {
    const groupServiceMock = {
      getGroup: vi.fn().mockResolvedValue({ id: 'group-1', name: 'Group', permissions: ['SEND_ENTRIES'] }),
      findAllMembers: vi.fn().mockResolvedValue([]),
    };
    const groupDebtServiceMock = {
      getWorkspace: vi.fn().mockResolvedValue({ balances: [] }),
      listHistory: vi.fn().mockResolvedValue([]),
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
              queryParamMap: convertToParamMap(monthQuery == null ? {} : { month: monthQuery }),
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

  it('loads with current month selected by default', async () => {
    const { component, groupDebtServiceMock } = await setup();
    const expectedMonth = dayjs(component.selectedMonth()).format('YYYY-MM');

    expect(groupDebtServiceMock.getWorkspace).toHaveBeenCalledWith('group-1', expectedMonth);
    expect(expectedMonth).toBe(dayjs().format('YYYY-MM'));
  });

  it('updates selected month and reloads debts when user changes month', async () => {
    const { component, groupDebtServiceMock, routerMock } = await setup('2026-04');

    expect(groupDebtServiceMock.getWorkspace).toHaveBeenCalledWith('group-1', '2026-04');

    await component.onSelectedMonthChange(new Date(2026, 5, 20));

    expect(routerMock.navigate).toHaveBeenCalledWith(
      [],
      expect.objectContaining({
        queryParams: { month: '2026-06' },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      }),
    );
    expect(groupDebtServiceMock.getWorkspace).toHaveBeenLastCalledWith('group-1', '2026-06');
  });
});

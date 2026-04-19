import '@angular/compiler';
import { type ActivatedRoute, type Router, convertToParamMap } from '@angular/router';

import { Subject } from 'rxjs';

import dayjs from 'dayjs';
import { describe, expect, it, vi } from 'vitest';

import { FinancesOverviewPageComponent } from './finances-overview-page.component';

describe('FinancesOverviewPageComponent', () => {
  it('loads overview only once on initialization', async () => {
    const overviewDashboardServiceMock = {
      getOverview: vi.fn().mockResolvedValue({
        selectedMonth: '04-2026',
        currency: 'BRL',
        cards: [],
        charts: {
          balance: [{ month: '04-2026', value: 100, executedValue: 80, projectedValue: 20 }],
          cashIn: [],
          cashOut: [],
          expense: [],
          cashInByCategory: [],
          cashOutByCategory: [],
          expenseByGroup: [],
          expenseByCategory: [],
        },
        goalCommittedTotal: 0,
        freeBalanceTotal: 0,
        goalOverCommittedWarning: false,
      }),
    };
    const routeMock = {
      snapshot: {
        queryParamMap: convertToParamMap({}),
      },
    } as ActivatedRoute;
    const routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
    } as unknown as Router;
    const userActionEventServiceMock = {
      transactionInserted$: new Subject(),
      transactionUpdated$: new Subject(),
      transactionDeleted$: new Subject(),
      resyncRequired$: new Subject<void>(),
    };
    const errorMessageServiceMock = {
      handleError: vi.fn(),
    };
    const messageServiceMock = {};

    const component = new FinancesOverviewPageComponent(
      routeMock,
      routerMock,
      overviewDashboardServiceMock as never,
      userActionEventServiceMock as never,
      errorMessageServiceMock as never,
      messageServiceMock as never,
    );

    await Promise.resolve();
    await Promise.resolve();

    expect(overviewDashboardServiceMock.getOverview).toHaveBeenCalledTimes(1);
  });

  it('refreshes overview when receiving group wallet event without group filter', async () => {
    vi.useFakeTimers();
    try {
      const transactionInserted$ = new Subject<any>();
      const overviewDashboardServiceMock = {
        getOverview: vi.fn().mockResolvedValue({
          selectedMonth: '04-2026',
          currency: 'BRL',
          cards: [],
          charts: {
            balance: [],
            cashIn: [],
            cashOut: [],
            expense: [],
            cashInByCategory: [],
            cashOutByCategory: [],
            expenseByGroup: [],
            expenseByCategory: [],
          },
          goalCommittedTotal: 0,
          freeBalanceTotal: 0,
          goalOverCommittedWarning: false,
        }),
      };
      const routeMock = {
        snapshot: {
          queryParamMap: convertToParamMap({}),
        },
      } as ActivatedRoute;
      const routerMock = {
        navigate: vi.fn().mockResolvedValue(true),
      } as unknown as Router;
      const userActionEventServiceMock = {
        transactionInserted$,
        transactionUpdated$: new Subject(),
        transactionDeleted$: new Subject(),
        resyncRequired$: new Subject<void>(),
      };
      const errorMessageServiceMock = {
        handleError: vi.fn(),
      };
      const messageServiceMock = {};

      new FinancesOverviewPageComponent(
        routeMock,
        routerMock,
        overviewDashboardServiceMock as never,
        userActionEventServiceMock as never,
        errorMessageServiceMock as never,
        messageServiceMock as never,
      );

      await Promise.resolve();
      await Promise.resolve();
      expect(overviewDashboardServiceMock.getOverview).toHaveBeenCalledTimes(1);

      transactionInserted$.next({
        id: 'tx-group-1',
        type: 'TRANSFER',
        date: dayjs().format('YYYY-MM-DD'),
        entries: [],
        group: { id: 'group-1' },
      });

      await vi.advanceTimersByTimeAsync(300);
      await Promise.resolve();

      expect(overviewDashboardServiceMock.getOverview).toHaveBeenCalledTimes(2);
    } finally {
      vi.useRealTimers();
    }
  });
});

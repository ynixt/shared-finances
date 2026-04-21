import '@angular/compiler';
import { type ActivatedRoute, type Router, convertToParamMap } from '@angular/router';

import { Subject, of } from 'rxjs';

import dayjs from 'dayjs';
import { describe, expect, it, vi } from 'vitest';

import { DashboardFeedFilters, EMPTY_DASHBOARD_FEED_FILTERS } from '../../components/dashboard-filters/dashboard-feed-filters.model';
import { OverviewGroupPageComponent } from './overview-group-page.component';

describe('OverviewGroupPageComponent', () => {
  const createDashboard = () => ({
    selectedMonth: '04-2026',
    currency: 'BRL',
    cards: [],
    charts: {
      cashIn: { total: [], byMember: [] },
      expense: { total: [], byMember: [] },
      cashInByCategoryTotal: [],
      cashInByCategoryByMember: [],
      expenseByCategory: [],
      expenseByCategoryByMember: [],
      expenseByMember: [],
    },
    debtPairs: [],
    goalOverCommittedWarning: false,
  });

  const createGroup = () => ({
    id: 'group-1',
    name: 'Group One',
    role: 'EDITOR',
    permissions: [],
    itemsAssociated: [],
  });

  it('loads group overview only once on initialization', async () => {
    const overviewDashboardServiceMock = {
      getGroupOverview: vi.fn().mockResolvedValue(createDashboard()),
    };
    const groupServiceMock = {
      getGroup: vi.fn().mockResolvedValue(createGroup()),
      findAllMembers: vi.fn().mockResolvedValue([]),
    };
    const groupWalletItemServiceMock = {
      getAllItems: vi.fn().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 500 }),
    };
    const groupCategoriesServiceMock = {
      getAllCategories: vi.fn().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 }),
    };
    const routeMock = {
      snapshot: { queryParamMap: convertToParamMap({}) },
      paramMap: of(convertToParamMap({ id: 'group-1' })),
    } as ActivatedRoute;
    const routerMock = {
      navigateByUrl: vi.fn().mockResolvedValue(true),
      navigate: vi.fn().mockResolvedValue(true),
    } as unknown as Router;
    const groupsActionEventServiceMock = {
      groupUpdated$: new Subject(),
      groupDeleted$: new Subject(),
    };
    const userActionEventServiceMock = {
      transactionInserted$: new Subject(),
      transactionUpdated$: new Subject(),
      transactionDeleted$: new Subject(),
      resyncRequired$: new Subject<void>(),
    };
    const errorMessageServiceMock = { handleError: vi.fn() };
    const messageServiceMock = {};

    new OverviewGroupPageComponent(
      routerMock,
      routeMock,
      groupServiceMock as never,
      groupWalletItemServiceMock as never,
      groupCategoriesServiceMock as never,
      groupsActionEventServiceMock as never,
      overviewDashboardServiceMock as never,
      userActionEventServiceMock as never,
      messageServiceMock as never,
      errorMessageServiceMock as never,
    );

    await Promise.resolve();
    await Promise.resolve();

    expect(overviewDashboardServiceMock.getGroupOverview).toHaveBeenCalledTimes(1);
  });

  it('refreshes group dashboard when receiving relevant wallet event', async () => {
    vi.useFakeTimers();
    try {
      const transactionInserted$ = new Subject<any>();
      const overviewDashboardServiceMock = {
        getGroupOverview: vi.fn().mockResolvedValue(createDashboard()),
      };
      const groupServiceMock = {
        getGroup: vi.fn().mockResolvedValue(createGroup()),
        findAllMembers: vi.fn().mockResolvedValue([]),
      };
      const routeMock = {
        snapshot: { queryParamMap: convertToParamMap({}) },
        paramMap: of(convertToParamMap({ id: 'group-1' })),
      } as ActivatedRoute;
      const routerMock = {
        navigateByUrl: vi.fn().mockResolvedValue(true),
        navigate: vi.fn().mockResolvedValue(true),
      } as unknown as Router;
      const groupsActionEventServiceMock = {
        groupUpdated$: new Subject(),
        groupDeleted$: new Subject(),
      };
      const groupWalletItemServiceMock = {
        getAllItems: vi.fn().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 500 }),
      };
      const groupCategoriesServiceMock = {
        getAllCategories: vi.fn().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 }),
      };
      const userActionEventServiceMock = {
        transactionInserted$,
        transactionUpdated$: new Subject(),
        transactionDeleted$: new Subject(),
        resyncRequired$: new Subject<void>(),
      };
      const errorMessageServiceMock = { handleError: vi.fn() };
      const messageServiceMock = {};

      new OverviewGroupPageComponent(
        routerMock,
        routeMock,
        groupServiceMock as never,
        groupWalletItemServiceMock as never,
        groupCategoriesServiceMock as never,
        groupsActionEventServiceMock as never,
        overviewDashboardServiceMock as never,
        userActionEventServiceMock as never,
        messageServiceMock as never,
        errorMessageServiceMock as never,
      );

      await Promise.resolve();
      await Promise.resolve();
      expect(overviewDashboardServiceMock.getGroupOverview).toHaveBeenCalledTimes(1);

      transactionInserted$.next({
        id: 'tx-group-1',
        type: 'EXPENSE',
        date: dayjs().format('YYYY-MM-DD'),
        entries: [],
        group: { id: 'group-1' },
      });

      await vi.advanceTimersByTimeAsync(300);
      await Promise.resolve();
      expect(overviewDashboardServiceMock.getGroupOverview).toHaveBeenCalledTimes(2);
    } finally {
      vi.useRealTimers();
    }
  });

  it('updates only feed refresh key when filters change', async () => {
    const overviewDashboardServiceMock = {
      getGroupOverview: vi.fn().mockResolvedValue(createDashboard()),
    };
    const groupServiceMock = {
      getGroup: vi.fn().mockResolvedValue(createGroup()),
      findAllMembers: vi.fn().mockResolvedValue([]),
    };
    const groupWalletItemServiceMock = {
      getAllItems: vi.fn().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 500 }),
    };
    const groupCategoriesServiceMock = {
      getAllCategories: vi.fn().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 }),
    };
    const routeMock = {
      snapshot: { queryParamMap: convertToParamMap({}) },
      paramMap: of(convertToParamMap({ id: 'group-1' })),
    } as ActivatedRoute;
    const routerMock = {
      navigateByUrl: vi.fn().mockResolvedValue(true),
      navigate: vi.fn().mockResolvedValue(true),
    } as unknown as Router;
    const groupsActionEventServiceMock = {
      groupUpdated$: new Subject(),
      groupDeleted$: new Subject(),
    };
    const userActionEventServiceMock = {
      transactionInserted$: new Subject(),
      transactionUpdated$: new Subject(),
      transactionDeleted$: new Subject(),
      resyncRequired$: new Subject<void>(),
    };
    const errorMessageServiceMock = { handleError: vi.fn() };
    const messageServiceMock = {};

    const component = new OverviewGroupPageComponent(
      routerMock,
      routeMock,
      groupServiceMock as never,
      groupWalletItemServiceMock as never,
      groupCategoriesServiceMock as never,
      groupsActionEventServiceMock as never,
      overviewDashboardServiceMock as never,
      userActionEventServiceMock as never,
      messageServiceMock as never,
      errorMessageServiceMock as never,
    );

    await Promise.resolve();
    await Promise.resolve();
    const initialCallCount = overviewDashboardServiceMock.getGroupOverview.mock.calls.length;
    const filters: DashboardFeedFilters = { ...EMPTY_DASHBOARD_FEED_FILTERS, memberIds: ['user-1'] };

    component.onFiltersChange(filters);

    expect(component.tableRefreshKey).toBe(1);
    expect(overviewDashboardServiceMock.getGroupOverview).toHaveBeenCalledTimes(initialCallCount);
  });
});

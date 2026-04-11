import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { OverviewDashboardChartsComponent } from './overview-dashboard-charts.component';

describe('OverviewDashboardChartsComponent', () => {
  const localCurrencyPipeServiceMock = {
    transform: vi.fn((value: number, currency?: string) => `${currency ?? 'BRL'} ${value.toFixed(2)}`),
  };

  const translateServiceMock = {
    instant: vi.fn((key: string) => {
      const translations: Record<string, string> = {
        'financesPage.overviewPage.charts.fallbackSliceLabels.individual': 'Individual',
        'financesPage.overviewPage.charts.fallbackSliceLabels.uncategorized': 'Uncategorized',
        'financesPage.overviewPage.charts.fallbackSliceLabels.others': 'Others',
      };

      return translations[key] ?? key;
    }),
  };

  let component: OverviewDashboardChartsComponent;
  const injector = Injector.create({ providers: [] });

  beforeEach(() => {
    vi.clearAllMocks();
    component = runInInjectionContext(
      injector,
      () => new OverviewDashboardChartsComponent(localCurrencyPipeServiceMock as never, translateServiceMock as never),
    );
    (component as unknown as { charts: () => unknown }).charts = () => ({
      balance: [],
      cashIn: [],
      cashOut: [],
      expense: [],
      cashInByCategory: [],
      cashOutByCategory: [],
      expenseByGroup: [],
      expenseByCategory: [],
    });
    (component as unknown as { currency: () => string }).currency = () => 'BRL';
  });

  it('builds pie chart data using backend-provided slice order', () => {
    (component as unknown as { charts: () => unknown }).charts = () => ({
      balance: [],
      cashIn: [],
      cashOut: [],
      expense: [],
      cashInByCategory: [
        { id: 'cat-in-1', label: 'Salário', value: 60 },
        { id: null, label: 'PREDEFINED_OTHERS', value: 10 },
      ],
      cashOutByCategory: [
        { id: 'cat-out-1', label: 'Fatura', value: 35 },
        { id: null, label: 'PREDEFINED_UNCATEGORIZED', value: 5 },
      ],
      expenseByGroup: [
        { id: 'group-1', label: 'Moradia', value: 40 },
        { id: null, label: 'PREDEFINED_OTHERS', value: 15 },
        { id: null, label: 'PREDEFINED_INDIVIDUAL', value: 5 },
      ],
      expenseByCategory: [
        { id: 'cat-1', label: 'Mercado', value: 25 },
        { id: null, label: 'PREDEFINED_UNCATEGORIZED', value: 5 },
      ],
    });

    expect(component.cashInByCategoryChartData().labels).toEqual(['Salário', 'Others']);
    expect(component.cashInByCategoryChartData().datasets[0]?.data).toEqual([60, 10]);
    expect(component.cashOutByCategoryChartData().labels).toEqual(['Fatura', 'Uncategorized']);
    expect(component.cashOutByCategoryChartData().datasets[0]?.data).toEqual([35, 5]);
    expect(component.expenseByGroupChartData().labels).toEqual(['Moradia', 'Others', 'Individual']);
    expect(component.expenseByGroupChartData().datasets[0]?.data).toEqual([40, 15, 5]);
    expect(component.expenseByCategoryChartData().labels).toEqual(['Mercado', 'Uncategorized']);
    expect(component.expenseByCategoryChartData().datasets[0]?.data).toEqual([25, 5]);
  });

  it('reports empty state when no breakdown slices are available', () => {
    expect(component.hasCashInByCategoryData()).toBe(false);
    expect(component.hasCashOutByCategoryData()).toBe(false);
    expect(component.hasExpenseByGroupData()).toBe(false);
    expect(component.hasExpenseByCategoryData()).toBe(false);
  });

  it('reuses stable chart references while inputs do not change', () => {
    (component as unknown as { charts: () => unknown }).charts = () => ({
      balance: [{ month: '04-2026', value: 100 }],
      cashIn: [],
      cashOut: [],
      expense: [],
      cashInByCategory: [],
      cashOutByCategory: [],
      expenseByGroup: [{ id: 'group-1', label: 'Moradia', value: 40 }],
      expenseByCategory: [],
    });

    expect(component.balanceChartData()).toBe(component.balanceChartData());
    expect(component.expenseByGroupChartData()).toBe(component.expenseByGroupChartData());
    expect(component.lineChartOptions).toBe(component.lineChartOptions);
    expect(component.pieChartOptions).toBe(component.pieChartOptions);
  });
});

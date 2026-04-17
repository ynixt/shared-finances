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
        { id: 'cat-in-1', label: 'Salário', value: 60, executedValue: 50, projectedValue: 10 },
        { id: null, label: 'PREDEFINED_OTHERS', value: 10, executedValue: 6, projectedValue: 4 },
      ],
      cashOutByCategory: [
        { id: 'cat-out-1', label: 'Fatura', value: 35, executedValue: 20, projectedValue: 15 },
        { id: null, label: 'PREDEFINED_UNCATEGORIZED', value: 5, executedValue: 5, projectedValue: 0 },
      ],
      expenseByGroup: [
        { id: 'group-1', label: 'Moradia', value: 40, executedValue: 25, projectedValue: 15 },
        { id: null, label: 'PREDEFINED_OTHERS', value: 15, executedValue: 10, projectedValue: 5 },
        { id: null, label: 'PREDEFINED_INDIVIDUAL', value: 5, executedValue: 5, projectedValue: 0 },
      ],
      expenseByCategory: [
        { id: 'cat-1', label: 'Mercado', value: 25, executedValue: 15, projectedValue: 10 },
        { id: null, label: 'PREDEFINED_UNCATEGORIZED', value: 5, executedValue: 2, projectedValue: 3 },
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
      balance: [{ month: '04-2026', value: 100, executedValue: 100, projectedValue: 0 }],
      cashIn: [],
      cashOut: [],
      expense: [],
      cashInByCategory: [],
      cashOutByCategory: [],
      expenseByGroup: [{ id: 'group-1', label: 'Moradia', value: 40, executedValue: 20, projectedValue: 20 }],
      expenseByCategory: [],
    });

    expect(component.balanceChartData()).toBe(component.balanceChartData());
    expect(component.expenseByGroupChartData()).toBe(component.expenseByGroupChartData());
    expect(component.lineChartOptions).toBe(component.lineChartOptions);
    expect(component.pieChartOptions).toBe(component.pieChartOptions);
  });

  it('composes line values from executed and projected components per chart', () => {
    (component as unknown as { charts: () => unknown }).charts = () => ({
      balance: [{ month: '04-2026', value: 130, executedValue: 100, projectedValue: 30 }],
      cashIn: [{ month: '04-2026', value: 250, executedValue: 200, projectedValue: 50 }],
      cashOut: [],
      expense: [],
      cashInByCategory: [],
      cashOutByCategory: [],
      expenseByGroup: [],
      expenseByCategory: [],
    });

    expect(component.balanceChartData().datasets[0]?.data).toEqual([130]);
    expect(component.cashInChartData().datasets[0]?.data).toEqual([250]);

    component.includeBalanceProjected.set(false);
    expect(component.balanceChartData().datasets[0]?.data).toEqual([100]);
    expect(component.cashInChartData().datasets[0]?.data).toEqual([250]);

    component.includeCashInProjected.set(false);
    expect(component.cashInChartData().datasets[0]?.data).toEqual([200]);
  });

  it('keeps line y-axis max stable when toggling projected values', () => {
    (component as unknown as { charts: () => unknown }).charts = () => ({
      balance: [{ month: '04-2026', value: 130, executedValue: 100, projectedValue: 30 }],
      cashIn: [],
      cashOut: [],
      expense: [],
      cashInByCategory: [],
      cashOutByCategory: [],
      expenseByGroup: [],
      expenseByCategory: [],
    });

    expect((component.balanceLineChartOptions().scales?.y as { max?: number } | undefined)?.max).toBe(130);
    component.includeBalanceProjected.set(false);
    expect((component.balanceLineChartOptions().scales?.y as { max?: number } | undefined)?.max).toBe(130);
  });

  it('applies projected toggle independently for pie charts', () => {
    (component as unknown as { charts: () => unknown }).charts = () => ({
      balance: [],
      cashIn: [],
      cashOut: [],
      expense: [],
      cashInByCategory: [{ id: 'cat-in-1', label: 'Salário', value: 12, executedValue: 0, projectedValue: 12 }],
      cashOutByCategory: [],
      expenseByGroup: [],
      expenseByCategory: [{ id: 'cat-exp-1', label: 'Mercado', value: 30, executedValue: 10, projectedValue: 20 }],
    });

    expect(component.cashInByCategoryChartData().datasets[0]?.data).toEqual([12]);
    expect(component.expenseByCategoryChartData().datasets[0]?.data).toEqual([30]);

    component.includeExpenseByCategoryProjected.set(false);
    expect(component.expenseByCategoryChartData().datasets[0]?.data).toEqual([10]);
    expect(component.cashInByCategoryChartData().datasets[0]?.data).toEqual([12]);

    component.includeCashInByCategoryProjected.set(false);
    expect(component.hasCashInByCategoryData()).toBe(false);
  });

  it('keeps uncategorized and individual slices visible when projected toggle is off', () => {
    (component as unknown as { charts: () => unknown }).charts = () => ({
      balance: [],
      cashIn: [],
      cashOut: [],
      expense: [],
      cashInByCategory: [],
      cashOutByCategory: [],
      expenseByGroup: [{ id: null, label: 'PREDEFINED_INDIVIDUAL', value: 8, executedValue: 0, projectedValue: 8 }],
      expenseByCategory: [{ id: null, label: 'PREDEFINED_UNCATEGORIZED', value: 6, executedValue: 0, projectedValue: 6 }],
    });

    component.includeExpenseByGroupProjected.set(false);
    component.includeExpenseByCategoryProjected.set(false);

    expect(component.expenseByGroupChartData().labels).toEqual(['Individual']);
    expect(component.expenseByGroupChartData().datasets[0]?.data).toEqual([0]);
    expect(component.expenseByCategoryChartData().labels).toEqual(['Uncategorized']);
    expect(component.expenseByCategoryChartData().datasets[0]?.data).toEqual([0]);
  });
});

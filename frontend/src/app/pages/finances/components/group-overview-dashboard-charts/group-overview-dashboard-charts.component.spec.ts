import '@angular/compiler';
import { Injector, runInInjectionContext, ɵChangeDetectionScheduler, ɵEffectScheduler } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { describe, expect, it } from 'vitest';

import { GroupOverviewDashboardChartsDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/dashboard';
import { LocalCurrencyPipeService } from '../../../../pipes/local-currency.pipe';
import { LocalDatePipeService } from '../../../../pipes/local-date.pipe';
import { GroupOverviewDashboardChartsComponent } from './group-overview-dashboard-charts.component';

describe('GroupOverviewDashboardChartsComponent', () => {
  const createCharts = (): GroupOverviewDashboardChartsDto => ({
    cashIn: {
      total: [{ month: '04-2026', value: 125, executedValue: 100, projectedValue: 25 }],
      byMember: [
        {
          memberId: 'member-z',
          memberName: 'Zara',
          points: [{ month: '04-2026', value: 10, executedValue: 10, projectedValue: 0 }],
        },
        {
          memberId: 'member-a',
          memberName: 'Ana',
          points: [{ month: '04-2026', value: 15, executedValue: 15, projectedValue: 0 }],
        },
      ],
    },
    expense: {
      total: [{ month: '04-2026', value: 90, executedValue: 80, projectedValue: 10 }],
      byMember: [
        {
          memberId: 'member-z',
          memberName: 'Zara',
          points: [{ month: '04-2026', value: 6, executedValue: 6, projectedValue: 0 }],
        },
        {
          memberId: 'member-a',
          memberName: 'Ana',
          points: [{ month: '04-2026', value: 4, executedValue: 4, projectedValue: 0 }],
        },
      ],
    },
    cashInByCategoryTotal: [{ id: null, label: 'Category A', value: 60, executedValue: 40, projectedValue: 20 }],
    cashInByCategoryByMember: [
      {
        memberId: 'member-z',
        memberName: 'Zara',
        slices: [{ id: 'c1', label: 'Cat', value: 10, executedValue: 10, projectedValue: 0 }],
      },
      {
        memberId: 'member-a',
        memberName: 'Ana',
        slices: [{ id: 'c1', label: 'Cat', value: 20, executedValue: 20, projectedValue: 0 }],
      },
    ],
    expenseByCategory: [{ id: null, label: 'Category B', value: 50, executedValue: 40, projectedValue: 10 }],
    expenseByCategoryByMember: [],
    expenseByMember: [{ id: 'member-a', label: 'Ana', value: 50, executedValue: 40, projectedValue: 10 }],
  });

  const createComponent = () => {
    const injector = Injector.create({
      providers: [
        {
          provide: ɵChangeDetectionScheduler,
          useValue: {
            notify: () => undefined,
            runningTick: false,
          },
        },
        {
          provide: ɵEffectScheduler,
          useValue: {
            add: () => undefined,
            schedule: () => undefined,
            flush: () => undefined,
            remove: () => undefined,
          },
        },
        {
          provide: LocalCurrencyPipeService,
          useValue: {
            transform: (value: number) => `${value}`,
          } satisfies Pick<LocalCurrencyPipeService, 'transform'>,
        },
        {
          provide: LocalDatePipeService,
          useValue: {
            transform: () => 'Apr 26',
          } satisfies Pick<LocalDatePipeService, 'transform'>,
        },
        {
          provide: TranslateService,
          useValue: {
            instant: (value: string) => value,
          } satisfies Pick<TranslateService, 'instant'>,
        },
      ],
    });

    return runInInjectionContext(injector, () => new GroupOverviewDashboardChartsComponent());
  };

  it('recomputes line chart totals locally when include projected is toggled', () => {
    const component = createComponent();
    const points = createCharts().cashIn.total;

    const withoutProjected = (component as any).buildLineChartData(points, '#059669', 'rgba(16, 185, 129, 0.16)', false);
    const withProjected = (component as any).buildLineChartData(points, '#059669', 'rgba(16, 185, 129, 0.16)', true);

    expect(withoutProjected.datasets[0].data[0]).toBe(100);
    expect(withProjected.datasets[0].data[0]).toBe(125);
  });

  it('recomputes pie slices locally when include projected is toggled', () => {
    const component = createComponent();
    const slices = createCharts().expenseByCategory;

    const withoutProjected = (component as any).buildPieSlices(slices, false);
    const withProjected = (component as any).buildPieSlices(slices, true);

    expect(withoutProjected[0].value).toBe(40);
    expect(withProjected[0].value).toBe(50);
  });

  it('sorts member datasets alphabetically in frontend', () => {
    const component = createComponent();
    const charts = createCharts();

    const sortedSeries = (component as any).buildMemberSeries(charts.cashIn.byMember, '#0ea5e9', 'rgba(14, 165, 233, 0.16)', false);
    const sortedPies = (component as any).buildMemberPies(charts.cashInByCategoryByMember, false);

    expect(sortedSeries.map((item: { memberName: string }) => item.memberName)).toEqual(['Ana', 'Zara']);
    expect(sortedPies.map((item: { memberName: string }) => item.memberName)).toEqual(['Ana', 'Zara']);
  });

  it('keeps every group member in expense-by-member pie even with zero expense', () => {
    const component = createComponent();
    const charts = createCharts();

    const slices = (component as any).buildExpenseByMemberPie(charts, false);

    expect(slices).toEqual([
      { id: 'member-a', label: 'Ana', value: 40 },
      { id: 'member-z', label: 'Zara', value: 0 },
    ]);
  });
});

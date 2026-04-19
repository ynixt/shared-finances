import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ChartData, ChartOptions, TooltipItem } from 'chart.js';
import 'chart.js/auto';
import dayjs from 'dayjs';
import { ChartModule } from 'primeng/chart';
import { ToggleSwitch } from 'primeng/toggleswitch';

import {
  OverviewDashboardChartPointDto,
  OverviewDashboardChartsDto,
  OverviewDashboardPieSliceDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/dashboard';
import { LocalCurrencyPipeService } from '../../../../pipes/local-currency.pipe';
import { LocalDatePipeService } from '../../../../pipes/local-date.pipe';
import { capitalize } from '../../../../util/string-util';
import { DateRange } from '../wallet-entry-table/components/advanced-date-picker/advanced-date-picker.component';

@Component({
  selector: 'app-overview-dashboard-charts',
  imports: [TranslatePipe, FormsModule, ChartModule, ToggleSwitch],
  templateUrl: './overview-dashboard-charts.component.html',
})
export class OverviewDashboardChartsComponent {
  private readonly localCurrencyPipeService = inject(LocalCurrencyPipeService);
  private readonly translateService = inject(TranslateService);
  private readonly localDatePipeService = inject(LocalDatePipeService);

  private readonly piePalette = [
    '#e11d48',
    '#f97316',
    '#f59e0b',
    '#84cc16',
    '#14b8a6',
    '#0ea5e9',
    '#6366f1',
    '#8b5cf6',
    '#d946ef',
    '#64748b',
  ];
  private readonly expenseCategoryPersistentLabels = new Set(['PREDEFINED_UNCATEGORIZED']);
  private readonly expenseGroupPersistentLabels = new Set(['PREDEFINED_INDIVIDUAL']);

  readonly charts = input<OverviewDashboardChartsDto | undefined>(undefined);
  readonly currency = input<string | undefined>(undefined);
  /** Month selection from the overview page (same as `dateControl` there); drives default projected toggles by month/year vs today. */
  readonly overviewDateRange = input<DateRange | undefined | null>(undefined);
  readonly includeBalanceProjected = signal(false);
  readonly includeCashInProjected = signal(false);
  readonly includeCashOutProjected = signal(false);
  readonly includeExpenseProjected = signal(false);
  readonly includeCashInByCategoryProjected = signal(false);
  readonly includeCashOutByCategoryProjected = signal(false);
  readonly includeExpenseByGroupProjected = signal(false);
  readonly includeExpenseByCategoryProjected = signal(false);

  constructor() {
    effect(() => {
      const include = this.resolveIncludeProjectedDefault(this.overviewDateRange());
      this.includeBalanceProjected.set(include);
      this.includeCashInProjected.set(include);
      this.includeCashOutProjected.set(include);
      this.includeExpenseProjected.set(include);
      this.includeCashInByCategoryProjected.set(include);
      this.includeCashOutByCategoryProjected.set(include);
      this.includeExpenseByGroupProjected.set(include);
      this.includeExpenseByCategoryProjected.set(include);
    });
  }
  readonly lineChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        callbacks: {
          label: (context: TooltipItem<'line'>) => this.formatCurrency(context.parsed.y),
        },
      },
    },
    scales: {
      x: {
        grid: {
          display: false,
        },
        ticks: {
          color: '#64748b',
        },
      },
      y: {
        ticks: {
          color: '#64748b',
          callback: value => this.formatCurrency(value),
        },
        grid: {
          color: 'rgba(148, 163, 184, 0.18)',
        },
      },
    },
    elements: {
      line: {
        tension: 0.35,
        borderWidth: 3,
      },
      point: {
        radius: 3,
        hoverRadius: 5,
      },
    },
  };
  readonly pieChartOptions: ChartOptions<'pie'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: '#334155',
          usePointStyle: true,
          boxWidth: 10,
        },
      },
      tooltip: {
        callbacks: {
          label: context => {
            const value = typeof context.parsed === 'number' ? context.parsed : Number(context.raw);
            const datasetValues = context.dataset.data.map(item => Number(item) || 0);
            const total = datasetValues.reduce((acc, item) => acc + item, 0);
            const percentage = total <= 0 ? '0.0' : ((value / total) * 100).toFixed(1);
            return `${context.label}: ${this.formatCurrency(value)} (${percentage}%)`;
          },
        },
      },
    },
  };
  readonly balanceChartData = computed(() =>
    this.buildChartData(this.charts()?.balance ?? [], '#0284c7', 'rgba(14, 165, 233, 0.16)', this.includeBalanceProjected()),
  );
  readonly balanceLineChartOptions = computed(() => this.buildLineChartOptions(this.resolveLineAxisMax(this.charts()?.balance ?? [])));
  readonly cashInChartData = computed(() =>
    this.buildChartData(this.charts()?.cashIn ?? [], '#059669', 'rgba(16, 185, 129, 0.16)', this.includeCashInProjected()),
  );
  readonly cashInLineChartOptions = computed(() => this.buildLineChartOptions(this.resolveLineAxisMax(this.charts()?.cashIn ?? [])));
  readonly cashOutChartData = computed(() =>
    this.buildChartData(this.charts()?.cashOut ?? [], '#f97316', 'rgba(249, 115, 22, 0.16)', this.includeCashOutProjected()),
  );
  readonly cashOutLineChartOptions = computed(() => this.buildLineChartOptions(this.resolveLineAxisMax(this.charts()?.cashOut ?? [])));
  readonly expenseChartData = computed(() =>
    this.buildChartData(this.charts()?.expense ?? [], '#e11d48', 'rgba(244, 63, 94, 0.16)', this.includeExpenseProjected()),
  );
  readonly expenseLineChartOptions = computed(() => this.buildLineChartOptions(this.resolveLineAxisMax(this.charts()?.expense ?? [])));
  readonly cashInByCategorySlices = computed(() =>
    this.buildPieSlices(this.charts()?.cashInByCategory ?? [], this.includeCashInByCategoryProjected()),
  );
  readonly cashOutByCategorySlices = computed(() =>
    this.buildPieSlices(this.charts()?.cashOutByCategory ?? [], this.includeCashOutByCategoryProjected()),
  );
  readonly expenseByGroupSlices = computed(() =>
    this.buildPieSlices(this.charts()?.expenseByGroup ?? [], this.includeExpenseByGroupProjected(), this.expenseGroupPersistentLabels),
  );
  readonly expenseByCategorySlices = computed(() =>
    this.buildPieSlices(
      this.charts()?.expenseByCategory ?? [],
      this.includeExpenseByCategoryProjected(),
      this.expenseCategoryPersistentLabels,
    ),
  );
  readonly cashInByCategoryChartData = computed(() => this.buildPieChartData(this.cashInByCategorySlices()));
  readonly cashOutByCategoryChartData = computed(() => this.buildPieChartData(this.cashOutByCategorySlices()));
  readonly expenseByGroupChartData = computed(() => this.buildPieChartData(this.expenseByGroupSlices()));
  readonly expenseByCategoryChartData = computed(() => this.buildPieChartData(this.expenseByCategorySlices()));
  readonly hasCashInByCategoryData = computed(() => this.cashInByCategorySlices().length > 0);
  readonly hasCashOutByCategoryData = computed(() => this.cashOutByCategorySlices().length > 0);
  readonly hasExpenseByGroupData = computed(() => this.expenseByGroupSlices().length > 0);
  readonly hasExpenseByCategoryData = computed(() => this.expenseByCategorySlices().length > 0);

  private resolveIncludeProjectedDefault(dateRange: DateRange | undefined | null): boolean {
    if (dateRange?.startDate == null) {
      return false;
    }

    const selectedMonth = dateRange.startDate.startOf('month');
    const currentMonth = dayjs().startOf('month');
    return selectedMonth.isAfter(currentMonth, 'month');
  }

  private buildChartData(
    points: OverviewDashboardChartPointDto[],
    borderColor: string,
    backgroundColor: string,
    includeProjected: boolean,
  ): ChartData<'line'> {
    return {
      labels: points.map(point => this.monthLabel(point.month)),
      datasets: [
        {
          tension: 0,
          data: points.map(point => point.executedValue + (includeProjected ? point.projectedValue : 0)),
          borderColor,
          backgroundColor,
          fill: true,
        },
      ],
    };
  }

  private buildLineChartOptions(max: number | undefined): ChartOptions<'line'> {
    return {
      ...this.lineChartOptions,
      scales: {
        ...this.lineChartOptions.scales,
        y: {
          ...this.lineChartOptions.scales?.['y'],
          ...(max == null ? {} : { max }),
        },
      },
    };
  }

  private resolveLineAxisMax(points: OverviewDashboardChartPointDto[]): number | undefined {
    if (points.length === 0) {
      return undefined;
    }

    const max = points.reduce((currentMax, point) => {
      const withProjected = point.executedValue + point.projectedValue;
      return Math.max(currentMax, point.executedValue, withProjected);
    }, Number.NEGATIVE_INFINITY);

    if (Number.isFinite(max)) {
      if (max == 0) {
        return 50;
      }

      return max + max * 0.1;
    }

    return undefined;
  }

  private buildPieSlices(
    slices: OverviewDashboardPieSliceDto[],
    includeProjected: boolean,
    persistentLabels: ReadonlySet<string> = new Set<string>(),
  ): PieSlicePoint[] {
    return slices
      .map(slice => ({
        id: slice.id ?? null,
        label: slice.label,
        value: slice.executedValue + (includeProjected ? slice.projectedValue : 0),
      }))
      .filter(slice => slice.value > 0 || persistentLabels.has(slice.label));
  }

  private buildPieChartData(slices: PieSlicePoint[]): ChartData<'pie'> {
    return {
      labels: slices.map(slice => this.pieSliceLabel(slice)),
      datasets: [
        {
          data: slices.map(slice => slice.value),
          backgroundColor: slices.map((_, index) => this.piePalette[index % this.piePalette.length]),
          borderColor: '#ffffff',
          borderWidth: 2,
          hoverOffset: 8,
        },
      ],
    };
  }

  private monthLabel(month: string): string {
    const [m, y] = month.split('-');
    if (m == null || y == null) return month;

    return capitalize(this.localDatePipeService.transform(dayjs(`${y}-${m}-01`), 'MMM yy').replace('.', ''));
  }

  private formatCurrency(value: unknown): string {
    const numericValue = typeof value === 'number' ? value : Number(value);
    return this.localCurrencyPipeService.transform(Number.isNaN(numericValue) ? 0 : numericValue, this.currency());
  }

  private pieSliceLabel(slice: Pick<OverviewDashboardPieSliceDto, 'id' | 'label'>): string {
    const translationKey = this.breakdownFallbackLabelKey(slice.label);
    if (translationKey != null) {
      return this.translateService.instant(translationKey);
    }

    return slice.label;
  }

  private breakdownFallbackLabelKey(label: string): string | undefined {
    return {
      PREDEFINED_INDIVIDUAL: 'financesPage.overviewPage.charts.fallbackSliceLabels.individual',
      PREDEFINED_UNCATEGORIZED: 'financesPage.overviewPage.charts.fallbackSliceLabels.uncategorized',
      PREDEFINED_OTHERS: 'financesPage.overviewPage.charts.fallbackSliceLabels.others',
      PREDEFINED_SHARED_FINANCE_DEBT: 'financesPage.overviewPage.charts.fallbackSliceLabels.sharedFinanceDebt',
    }[label];
  }
}

type PieSlicePoint = {
  id?: string | null;
  label: string;
  value: number;
};

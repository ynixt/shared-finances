import { Component, computed, input } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ChartData, ChartOptions, TooltipItem } from 'chart.js';
import 'chart.js/auto';
import dayjs from 'dayjs';
import { ChartModule } from 'primeng/chart';

import {
  OverviewDashboardChartPointDto,
  OverviewDashboardChartsDto,
  OverviewDashboardPieSliceDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/dashboard';
import { LocalCurrencyPipeService } from '../../../../pipes/local-currency.pipe';

@Component({
  selector: 'app-overview-dashboard-charts',
  imports: [TranslatePipe, ChartModule],
  templateUrl: './overview-dashboard-charts.component.html',
})
export class OverviewDashboardChartsComponent {
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

  readonly charts = input<OverviewDashboardChartsDto | undefined>(undefined);
  readonly currency = input<string | undefined>(undefined);
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
  readonly balanceChartData = computed(() => this.buildChartData(this.charts()?.balance ?? [], '#0284c7', 'rgba(14, 165, 233, 0.16)'));
  readonly cashInChartData = computed(() => this.buildChartData(this.charts()?.cashIn ?? [], '#059669', 'rgba(16, 185, 129, 0.16)'));
  readonly cashOutChartData = computed(() => this.buildChartData(this.charts()?.cashOut ?? [], '#f97316', 'rgba(249, 115, 22, 0.16)'));
  readonly expenseChartData = computed(() => this.buildChartData(this.charts()?.expense ?? [], '#e11d48', 'rgba(244, 63, 94, 0.16)'));
  readonly cashInByCategoryChartData = computed(() => this.buildPieChartData(this.charts()?.cashInByCategory ?? []));
  readonly cashOutByCategoryChartData = computed(() => this.buildPieChartData(this.charts()?.cashOutByCategory ?? []));
  readonly expenseByGroupChartData = computed(() => this.buildPieChartData(this.charts()?.expenseByGroup ?? []));
  readonly expenseByCategoryChartData = computed(() => this.buildPieChartData(this.charts()?.expenseByCategory ?? []));
  readonly hasCashInByCategoryData = computed(() => (this.charts()?.cashInByCategory ?? []).length > 0);
  readonly hasCashOutByCategoryData = computed(() => (this.charts()?.cashOutByCategory ?? []).length > 0);
  readonly hasExpenseByGroupData = computed(() => (this.charts()?.expenseByGroup ?? []).length > 0);
  readonly hasExpenseByCategoryData = computed(() => (this.charts()?.expenseByCategory ?? []).length > 0);

  constructor(
    private readonly localCurrencyPipeService: LocalCurrencyPipeService,
    private readonly translateService: TranslateService,
  ) {}

  private buildChartData(points: OverviewDashboardChartPointDto[], borderColor: string, backgroundColor: string): ChartData<'line'> {
    return {
      labels: points.map(point => this.monthLabel(point.month)),
      datasets: [
        {
          data: points.map(point => point.value),
          borderColor,
          backgroundColor,
          fill: true,
        },
      ],
    };
  }

  private buildPieChartData(slices: OverviewDashboardPieSliceDto[]): ChartData<'pie'> {
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

    return dayjs(`${y}-${m}-01`).format('MMM/YY');
  }

  private formatCurrency(value: unknown): string {
    const numericValue = typeof value === 'number' ? value : Number(value);
    return this.localCurrencyPipeService.transform(Number.isNaN(numericValue) ? 0 : numericValue, this.currency());
  }

  private pieSliceLabel(slice: OverviewDashboardPieSliceDto): string {
    if (slice.id != null) {
      return slice.label;
    }

    const translationKey = this.breakdownFallbackLabelKey(slice.label);
    return translationKey == null ? slice.label : this.translateService.instant(translationKey);
  }

  private breakdownFallbackLabelKey(label: string): string | undefined {
    return {
      PREDEFINED_INDIVIDUAL: 'financesPage.overviewPage.charts.fallbackSliceLabels.individual',
      PREDEFINED_UNCATEGORIZED: 'financesPage.overviewPage.charts.fallbackSliceLabels.uncategorized',
      PREDEFINED_OTHERS: 'financesPage.overviewPage.charts.fallbackSliceLabels.others',
    }[label];
  }
}

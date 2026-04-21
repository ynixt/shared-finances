import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ChartData, ChartOptions, TooltipItem } from 'chart.js';
import 'chart.js/auto';
import dayjs from 'dayjs';
import { ChartModule } from 'primeng/chart';
import { ToggleSwitch } from 'primeng/toggleswitch';

import {
  GroupOverviewDashboardChartsDto,
  GroupOverviewDashboardMemberPieDto,
  GroupOverviewDashboardMemberSeriesDto,
  OverviewDashboardChartPointDto,
  OverviewDashboardPieSliceDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/dashboard';
import { LocalCurrencyPipeService } from '../../../../pipes/local-currency.pipe';
import { LocalDatePipeService } from '../../../../pipes/local-date.pipe';
import { capitalize } from '../../../../util/string-util';
import { DateRange } from '../wallet-entry-table/components/advanced-date-picker/advanced-date-picker.component';

@Component({
  selector: 'app-group-overview-dashboard-charts',
  imports: [TranslatePipe, FormsModule, ChartModule, ToggleSwitch],
  templateUrl: './group-overview-dashboard-charts.component.html',
})
export class GroupOverviewDashboardChartsComponent {
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

  readonly charts = input<GroupOverviewDashboardChartsDto | undefined>(undefined);
  readonly currency = input<string | undefined>(undefined);
  readonly overviewDateRange = input<DateRange | undefined | null>(undefined);

  readonly includeCashInProjected = signal(false);
  readonly includeCashInByMemberProjected = signal(false);
  readonly includeExpenseProjected = signal(false);
  readonly includeExpenseByMemberProjected = signal(false);
  readonly includeCashInByCategoryProjected = signal(false);
  readonly includeCashInByCategoryByMemberProjected = signal(false);
  readonly includeExpenseByCategoryProjected = signal(false);
  readonly includeExpenseByCategoryByMemberProjected = signal(false);
  readonly includeExpenseByMemberPieProjected = signal(false);

  constructor() {
    effect(() => {
      const include = this.resolveIncludeProjectedDefault(this.overviewDateRange());
      this.includeCashInProjected.set(include);
      this.includeCashInByMemberProjected.set(include);
      this.includeExpenseProjected.set(include);
      this.includeExpenseByMemberProjected.set(include);
      this.includeCashInByCategoryProjected.set(include);
      this.includeCashInByCategoryByMemberProjected.set(include);
      this.includeExpenseByCategoryProjected.set(include);
      this.includeExpenseByCategoryByMemberProjected.set(include);
      this.includeExpenseByMemberPieProjected.set(include);
    });
  }

  readonly lineChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (context: TooltipItem<'line'>) => this.formatCurrency(context.parsed.y),
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { color: '#64748b' },
      },
      y: {
        ticks: {
          color: '#64748b',
          callback: value => this.formatCurrency(value),
        },
        grid: { color: 'rgba(148, 163, 184, 0.18)' },
      },
    },
    elements: {
      line: { tension: 0.35, borderWidth: 3 },
      point: { radius: 3, hoverRadius: 5 },
    },
  };

  readonly pieChartOptions: ChartOptions<'pie'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: { color: '#334155', usePointStyle: true, boxWidth: 10 },
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

  readonly cashInTotalChartData = computed(() =>
    this.buildLineChartData(this.charts()?.cashIn.total ?? [], '#059669', 'rgba(16, 185, 129, 0.16)', this.includeCashInProjected()),
  );
  readonly expenseTotalChartData = computed(() =>
    this.buildLineChartData(this.charts()?.expense.total ?? [], '#e11d48', 'rgba(244, 63, 94, 0.16)', this.includeExpenseProjected()),
  );

  readonly cashInByMember = computed(() =>
    this.buildMemberSeries(
      this.charts()?.cashIn.byMember ?? [],
      '#0ea5e9',
      'rgba(14, 165, 233, 0.16)',
      this.includeCashInByMemberProjected(),
    ),
  );
  readonly expenseByMember = computed(() =>
    this.buildMemberSeries(
      this.charts()?.expense.byMember ?? [],
      '#f97316',
      'rgba(249, 115, 22, 0.16)',
      this.includeExpenseByMemberProjected(),
    ),
  );

  readonly cashInByCategoryTotal = computed(() =>
    this.buildPieSlices(this.charts()?.cashInByCategoryTotal ?? [], this.includeCashInByCategoryProjected()),
  );
  readonly cashInByCategoryByMember = computed(() =>
    this.buildMemberPies(this.charts()?.cashInByCategoryByMember ?? [], this.includeCashInByCategoryByMemberProjected()),
  );
  readonly expenseByCategory = computed(() =>
    this.buildPieSlices(this.charts()?.expenseByCategory ?? [], this.includeExpenseByCategoryProjected()),
  );
  readonly expenseByCategoryByMember = computed(() =>
    this.buildMemberPies(this.charts()?.expenseByCategoryByMember ?? [], this.includeExpenseByCategoryByMemberProjected()),
  );
  readonly expenseByMemberPie = computed(() => this.buildExpenseByMemberPie(this.charts(), this.includeExpenseByMemberPieProjected()));

  readonly cashInByCategoryTotalChartData = computed(() => this.buildPieChartData(this.cashInByCategoryTotal()));
  readonly expenseByCategoryChartData = computed(() => this.buildPieChartData(this.expenseByCategory()));
  readonly expenseByMemberChartData = computed(() => this.buildPieChartData(this.expenseByMemberPie()));

  readonly hasCashInByCategoryTotal = computed(() => this.cashInByCategoryTotal().length > 0);
  readonly hasExpenseByCategory = computed(() => this.expenseByCategory().length > 0);
  readonly hasExpenseByMember = computed(() => this.expenseByMemberPie().length > 0);

  private resolveIncludeProjectedDefault(dateRange: DateRange | undefined | null): boolean {
    if (dateRange?.startDate == null) {
      return false;
    }
    const selectedMonth = dateRange.startDate.startOf('month');
    const currentMonth = dayjs().startOf('month');
    return selectedMonth.isAfter(currentMonth, 'month');
  }

  private buildLineChartData(
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

  private buildMemberSeries(
    members: GroupOverviewDashboardMemberSeriesDto[],
    borderColor: string,
    backgroundColor: string,
    includeProjected: boolean,
  ): Array<{ memberId: string; memberName: string; chartData: ChartData<'line'> }> {
    return this.sortMembersByName(members).map(member => ({
      memberId: member.memberId,
      memberName: member.memberName,
      chartData: this.buildLineChartData(member.points, borderColor, backgroundColor, includeProjected),
    }));
  }

  private buildPieSlices(slices: OverviewDashboardPieSliceDto[], includeProjected: boolean, includeZeroValues = false): PieSlicePoint[] {
    return slices
      .map(slice => ({
        id: slice.id ?? null,
        label: slice.label,
        value: slice.executedValue + (includeProjected ? slice.projectedValue : 0),
      }))
      .filter(slice => includeZeroValues || slice.value > 0);
  }

  private buildMemberPies(
    members: GroupOverviewDashboardMemberPieDto[],
    includeProjected: boolean,
  ): Array<{ memberId: string; memberName: string; slices: PieSlicePoint[]; chartData: ChartData<'pie'>; hasData: boolean }> {
    return this.sortMembersByName(members).map(member => {
      const slices = this.buildPieSlices(member.slices, includeProjected);
      return {
        memberId: member.memberId,
        memberName: member.memberName,
        slices,
        chartData: this.buildPieChartData(slices),
        hasData: slices.length > 0,
      };
    });
  }

  private buildExpenseByMemberPie(charts: GroupOverviewDashboardChartsDto | undefined, includeProjected: boolean): PieSlicePoint[] {
    if (charts == null) {
      return [];
    }

    const allMembers = this.sortMembersByName(charts.expense.byMember);
    const sliceByMemberId = new Map(charts.expenseByMember.filter(slice => slice.id != null).map(slice => [slice.id!, slice]));

    return allMembers.map(member => {
      const existingSlice = sliceByMemberId.get(member.memberId);
      return {
        id: member.memberId,
        label: member.memberName,
        value: (existingSlice?.executedValue ?? 0) + (includeProjected ? (existingSlice?.projectedValue ?? 0) : 0),
      };
    });
  }

  private sortMembersByName<T extends { memberName: string }>(members: T[]): T[] {
    return [...members].sort((left, right) => left.memberName.localeCompare(right.memberName, undefined, { sensitivity: 'base' }));
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

  private pieSliceLabel(slice: Pick<OverviewDashboardPieSliceDto, 'label'>): string {
    const translationKey =
      {
        PREDEFINED_UNCATEGORIZED: 'financesPage.overviewPage.charts.fallbackSliceLabels.uncategorized',
        PREDEFINED_OTHERS: 'financesPage.overviewPage.charts.fallbackSliceLabels.others',
      }[slice.label] ?? undefined;

    if (translationKey != null) {
      return this.translateService.instant(translationKey);
    }
    return slice.label;
  }
}

type PieSlicePoint = {
  id?: string | null;
  label: string;
  value: number;
};

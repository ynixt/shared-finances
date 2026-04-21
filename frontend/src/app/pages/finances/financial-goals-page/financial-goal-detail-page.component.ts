import { KeyValuePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { faPencil } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ChartData, ChartOptions, TooltipItem } from 'chart.js';
import 'chart.js/auto';
import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { TableModule } from 'primeng/table';

import {
  FinancialGoalDetailDto,
  GoalCommitmentChartSeriesDto,
  GoalContributionScheduleDto,
  GoalLedgerMovementDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/goals';
import { GroupPermissions__Obj } from '../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { Page } from '../../../models/pagination';
import { LocalCurrencyPipe, LocalCurrencyPipeService } from '../../../pipes/local-currency.pipe';
import { LocalDatePipe, LocalDatePipeService } from '../../../pipes/local-date.pipe';
import { ErrorMessageService } from '../../../services/error-message.service';
import { LocaleService } from '../../../services/locale.service';
import { createEmptyPage } from '../../../services/pagination.service';
import { FinancesTitleBarComponent, FinancesTitleBarExtraButton } from '../components/finances-title-bar/finances-title-bar.component';
import { FinancialGoalService } from '../services/financial-goal.service';
import { GroupService } from '../services/group.service';
import { resolveGoalWorkspaceContext } from './goal-workspace-context';

interface GoalCommitmentChartViewModel {
  currency: string;
  data: ChartData<'line'>;
  options: ChartOptions<'line'>;
}

@UntilDestroy()
@Component({
  selector: 'app-financial-goal-detail-page',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    TableModule,
    ButtonDirective,
    RouterLink,
    LocalCurrencyPipe,
    LocalDatePipe,
    KeyValuePipe,
    ChartModule,
  ],
  templateUrl: './financial-goal-detail-page.component.html',
})
export class FinancialGoalDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  readonly workspace = resolveGoalWorkspaceContext(this.route);
  readonly detail = signal<FinancialGoalDetailDto | undefined>(undefined);
  readonly loading = signal(true);
  readonly loadingMovements = signal(false);
  readonly loadingSchedules = signal(false);
  readonly canManageGoal = signal(true);
  readonly movementPage = signal<Page<GoalLedgerMovementDto>>(createEmptyPage());
  readonly schedulePage = signal<Page<GoalContributionScheduleDto>>(createEmptyPage());
  readonly movementPageSize = 10;
  readonly schedulePageSize = 10;
  /** Bumped on language change so chart labels (datasets) stay translated without rebuilding every CD. */
  private readonly chartI18nTick = signal(0);
  private currentMovementPage = 0;
  private currentSchedulePage = 0;

  readonly commitmentChartViews = computed((): GoalCommitmentChartViewModel[] => {
    this.chartI18nTick();
    const list = this.detail()?.commitmentChart ?? [];
    return list
      .filter(s => s.points.length > 0)
      .map(series => ({
        currency: series.currency,
        data: this.buildCommitmentChartData(series),
        options: this.buildCommitmentChartOptions(series),
      }));
  });

  goalId = '';
  protected readonly editIcon = faPencil;

  constructor(
    private financialGoalService: FinancialGoalService,
    private groupService: GroupService,
    private messageService: MessageService,
    private errorMessageService: ErrorMessageService,
    private translateService: TranslateService,
    private localCurrencyPipeService: LocalCurrencyPipeService,
    private localDatePipeService: LocalDatePipeService,
    private localeService: LocaleService,
  ) {
    this.goalId = this.route.snapshot.paramMap.get('id') ?? '';
    this.translateService.onLangChange.pipe(untilDestroyed(this)).subscribe(() => this.chartI18nTick.update(n => n + 1));
    this.localeService.locale$.pipe(untilDestroyed(this)).subscribe(() => this.chartI18nTick.update(n => n + 1));
    void this.reload();
  }

  async reload() {
    this.loading.set(true);
    try {
      const d = await this.financialGoalService.getGoal(this.goalId);
      this.detail.set(d);
      await this.resolveManageGoalsPermission(d.goal.groupId ?? this.workspace.groupId);
      await Promise.all([this.loadMovements(0), this.loadSchedules(0)]);
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    } finally {
      this.loading.set(false);
    }
  }

  get closeRouterLink(): string[] {
    return this.workspace.goalsRoot;
  }

  goalActionRouterLink(action: string): string[] {
    return [...this.workspace.goalsRoot, this.goalId, action];
  }

  movementEditRouterLink(movementId: string): string[] {
    return [...this.workspace.goalsRoot, this.goalId, 'movements', movementId, 'edit'];
  }

  scheduleEditRouterLink(scheduleId: string): string[] {
    return [...this.workspace.goalsRoot, this.goalId, 'schedules', scheduleId, 'edit'];
  }

  onMovementLazyLoad(event: { first?: number }) {
    const first = event.first ?? 0;
    void this.loadMovements(Math.floor(first / this.movementPageSize));
  }

  onScheduleLazyLoad(event: { first?: number }) {
    const first = event.first ?? 0;
    void this.loadSchedules(Math.floor(first / this.schedulePageSize));
  }

  private async loadMovements(page: number) {
    this.loadingMovements.set(true);
    this.currentMovementPage = page;
    try {
      this.movementPage.set(
        await this.financialGoalService.listMovements(this.goalId, {
          page,
          size: this.movementPageSize,
          sort: [
            { property: 'movementDate', direction: 'DESC' },
            { property: 'id', direction: 'DESC' },
          ],
        }),
      );
    } finally {
      this.loadingMovements.set(false);
    }
  }

  private async loadSchedules(page: number) {
    this.loadingSchedules.set(true);
    this.currentSchedulePage = page;
    try {
      this.schedulePage.set(
        await this.financialGoalService.listSchedules(this.goalId, {
          page,
          size: this.schedulePageSize,
          sort: [
            { property: 'nextExecution', direction: 'ASC' },
            { property: 'id', direction: 'ASC' },
          ],
        }),
      );
    } finally {
      this.loadingSchedules.set(false);
    }
  }

  async deleteSchedule(scheduleId: string) {
    if (!this.canManageGoal()) {
      return;
    }

    try {
      await this.financialGoalService.deleteSchedule(this.goalId, scheduleId);
      await this.loadSchedules(this.currentSchedulePage);
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    }
  }

  movementKindLabel(kind: string): string {
    return `financesPage.goalsPage.movementKind.${kind}`;
  }

  editExtras(): FinancesTitleBarExtraButton[] {
    if (!this.canManageGoal()) {
      return [];
    }

    return [
      {
        routerLink: [...this.workspace.goalsRoot, this.goalId, 'edit'],
        rounded: true,
        tooltip: 'general.edit',
        icon: this.editIcon,
      },
    ];
  }

  private async resolveManageGoalsPermission(goalGroupId: string | null | undefined) {
    if (this.workspace.scope !== 'group') {
      this.canManageGoal.set(true);
      return;
    }

    if (goalGroupId == null || goalGroupId === '') {
      this.canManageGoal.set(false);
      return;
    }

    try {
      const group = await this.groupService.getGroup(goalGroupId);
      this.canManageGoal.set(group.permissions.includes(GroupPermissions__Obj.MANAGE_GOALS));
    } catch {
      this.canManageGoal.set(false);
    }
  }

  private buildCommitmentChartData(series: GoalCommitmentChartSeriesDto): ChartData<'line'> {
    const labels = series.points.map(p => this.commitmentChartMonthLabel(p.yearMonth));
    const committed = series.points.map(p => p.committedCumulative);
    const target = series.points.map(() => series.targetAmount);
    return {
      labels,
      datasets: [
        {
          label: this.translateService.instant('financesPage.goalsPage.commitmentChartCommitted'),
          data: committed,
          borderColor: '#0284c7',
          backgroundColor: 'rgba(14, 165, 233, 0.12)',
          fill: true,
          tension: 0.35,
          borderWidth: 2,
        },
        {
          label: this.translateService.instant('financesPage.goalsPage.commitmentChartTarget'),
          data: target,
          borderColor: '#64748b',
          backgroundColor: 'transparent',
          fill: false,
          borderDash: [6, 4],
          tension: 0,
          borderWidth: 2,
          pointRadius: 0,
        },
      ],
    };
  }

  private buildCommitmentChartOptions(series: GoalCommitmentChartSeriesDto): ChartOptions<'line'> {
    const currency = series.currency;
    const format = (value: unknown) => this.localCurrencyPipeService.transform(typeof value === 'number' ? value : Number(value), currency);
    const { min, max } = this.commitmentYAxisBounds(series);
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: true,
          position: 'bottom',
          labels: {
            color: '#334155',
            usePointStyle: true,
            boxWidth: 10,
          },
        },
        tooltip: {
          callbacks: {
            title: (items: TooltipItem<'line'>[]) => {
              const idx = items[0]?.dataIndex;
              const lbl = idx != null ? (series.points[idx]?.yearMonth ?? '') : '';
              return lbl ? this.commitmentChartMonthLabel(lbl) : '';
            },
            label: (context: TooltipItem<'line'>) => {
              const ds = context.dataset.label ?? '';
              const y = context.parsed.y;
              return `${ds}: ${format(y)}`;
            },
          },
        },
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { color: '#64748b' },
        },
        y: {
          min,
          max,
          ticks: {
            color: '#64748b',
            callback: value => format(value),
          },
          grid: { color: 'rgba(148, 163, 184, 0.18)' },
        },
      },
      elements: {
        line: { tension: 0.35 },
        point: { radius: 3, hoverRadius: 5 },
      },
    };
  }

  /** Keeps the target line inside the scale (with padding) even when committed is much smaller. */
  private commitmentYAxisBounds(series: GoalCommitmentChartSeriesDto): { min: number; max: number } {
    const committed = series.points.map(p => Number(p.committedCumulative) || 0);
    const cMin = committed.length ? Math.min(...committed) : 0;
    const cMax = committed.length ? Math.max(...committed) : 0;
    const target = Number(series.targetAmount) || 0;
    let low = Math.min(0, cMin, target);
    let high = Math.max(0, cMax, target);
    if (high <= low) {
      high = low + 1;
    }
    const pad = (high - low) * 0.08;
    return { min: low - pad, max: high + pad };
  }

  /** `yearMonth` from API is `YYYY-MM`; format with app locale via Angular date formatting. */
  private commitmentChartMonthLabel(yearMonth: string): string {
    const [y, m] = yearMonth.split('-');
    if (y == null || m == null) {
      return yearMonth;
    }
    const d = dayjs(`${y}-${m}-01`).toDate();
    return this.localDatePipeService.transform(d, 'MMM y');
  }
}

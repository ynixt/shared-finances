import { Component, inject } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowsRotate, faBan, faFlask, faTrash } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { filter } from 'rxjs';

import dayjs from 'dayjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { InputNumber } from 'primeng/inputnumber';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';

import { CurrencySelectorComponent } from '../../../components/currency-selector/currency-selector.component';
import { DatePickerComponent } from '../../../components/date-picker/date-picker.component';
import { GroupWithRoleDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import {
  CreateSimulationJobRequestDto,
  SimulationJobDto,
  SimulationJobStatusEventDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/simulationjobs';
import {
  ActionEventType__Obj,
  GroupPermissions__Obj,
  PlanningSimulationOutcomeBand,
  SimulationJobStatus,
  SimulationJobType__Obj,
} from '../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { Page } from '../../../models/pagination';
import { LocalCurrencyPipe } from '../../../pipes/local-currency.pipe';
import { LocalDatePipe } from '../../../pipes/local-date.pipe';
import { createEmptyPage } from '../../../services/pagination.service';
import { UserService } from '../../../services/user.service';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import { resolveGoalWorkspaceContext } from '../financial-goals-page/goal-workspace-context';
import { GroupService } from '../services/group.service';
import { SimulationJobService } from '../services/simulation-job.service';
import { UserActionEventService } from '../services/user-action-event.service';
import {
  PlanningDebtInputForm,
  PlanningSimulationCurrencyPoint,
  PlanningSimulationResultPayload,
  buildPlanningSimulationRequestPayload,
  defaultPlanningDebtForm,
  parsePlanningSimulationResultPayload,
} from './planning-simulation-payload';

function planningDebtAmountValidator(control: AbstractControl): ValidationErrors | null {
  const v = control.value;
  if (v == null) return { required: true };
  const n = typeof v === 'number' ? v : Number(v);
  if (!Number.isFinite(n) || n <= 0) return { positiveAmount: true };
  return null;
}

function isoCurrencyCodeValidator(control: AbstractControl): ValidationErrors | null {
  const raw = control.value;
  if (raw == null || typeof raw !== 'string') return { required: true };
  const v = raw.trim().toUpperCase();
  if (!/^[A-Z]{3}$/.test(v)) return { currencyCode: true };
  return null;
}

@UntilDestroy()
@Component({
  selector: 'app-simulation-jobs-common',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    ButtonDirective,
    Tag,
    Tooltip,
    FaIconComponent,
    LocalDatePipe,
    LocalCurrencyPipe,
    Message,
    CurrencySelectorComponent,
    InputNumber,
    ReactiveFormsModule,
    DatePickerComponent,
    ConfirmDialog,
  ],
  providers: [ConfirmationService],
  templateUrl: './simulation-jobs-common.component.html',
})
export class SimulationJobsCommonComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly simulationJobService = inject(SimulationJobService);
  private readonly userActionEventService = inject(UserActionEventService);
  private readonly userService = inject(UserService);
  private readonly groupService = inject(GroupService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translateService = inject(TranslateService);
  private readonly fb = inject(FormBuilder);

  protected readonly GroupPermissions__Obj = GroupPermissions__Obj;

  /** Horizonte da simulação — separado do formulário de inclusão de dívidas. */
  readonly simulationForm = this.fb.group({
    horizonMonths: [6, [Validators.required, Validators.min(1), Validators.max(480)]],
  });

  /** Limite inferior do horizonte (= maior número de parcelas entre as dívidas), para o template. */
  simulationHorizonMin = 1;

  readonly createIcon = faFlask;
  readonly refreshIcon = faArrowsRotate;
  readonly cancelIcon = faBan;
  readonly trashIcon = faTrash;
  readonly pageSize = 12;
  readonly workspace = resolveGoalWorkspaceContext(this.route);
  readonly closeRouterLink = this.workspace.goalsRoot;

  jobsPage: Page<SimulationJobDto> = createEmptyPage();
  loading = false;
  loadError = false;
  creating = false;
  createError = false;
  waitingAsyncResult = false;
  lastCreateCompletedFastPath = false;
  cancelling = new Set<string>();
  planningForm: FormGroup | null = null;
  debts: PlanningDebtInputForm[] = [];
  lastCreatedJobId: string | null = null;
  groupWithRole: GroupWithRoleDto | null = null;
  deleteError = false;
  removingSimulation = new Set<string>();
  private readonly parsedResultsByJobId = new Map<string, PlanningSimulationResultPayload | null>();

  constructor() {
    void this.loadPage(0);
    void this.createDefaultPlanningDebtForm();

    if (this.workspace.scope === 'group' && this.workspace.groupId != null) {
      void this.groupService.getGroup(this.workspace.groupId).then(g => (this.groupWithRole = g));
    }

    if (this.workspace.scope === 'group' && this.workspace.groupId != null) {
      this.userActionEventService.groupEvents$
        .pipe(
          filter(event => event.event === 'SIMULATION_JOB' && event.groupId === this.workspace.groupId),
          untilDestroyed(this),
        )
        .subscribe(event => {
          if (event.type === ActionEventType__Obj.DELETE) {
            const raw = event.data;
            const id = typeof raw === 'string' ? raw : (raw as { id?: string })?.id;
            if (id != null) {
              void this.loadPage(this.jobsPage.number ?? 0);
            }
            return;
          }
          const payload = event.data as SimulationJobStatusEventDto;
          void this.refreshJob(payload.id);
        });
    } else {
      this.userActionEventService.simulationJobAction$.pipe(untilDestroyed(this)).subscribe(event => {
        if (event.type === ActionEventType__Obj.DELETE) {
          const raw = event.data;
          const id = typeof raw === 'string' ? raw : (raw as { id?: string })?.id;
          if (id != null) {
            void this.loadPage(this.jobsPage.number ?? 0);
          }
          return;
        }
        const payload = event.data as SimulationJobStatusEventDto;
        void this.refreshJob(payload.id);
      });
    }

    this.userActionEventService.resyncRequired$.pipe(untilDestroyed(this)).subscribe(() => {
      void this.loadPage(this.jobsPage.number ?? 0);
    });
  }

  async createDefaultPlanningDebtForm() {
    const user = await this.userService.getUser();
    const defaults = defaultPlanningDebtForm(user!!);

    /** `app-date-picker` / PrimeNG expect native `Date`, not dayjs. */
    const defaultPaymentDate = defaults.firstPaymentDate.toDate();

    if (this.planningForm == null) {
      this.planningForm = this.fb.group({
        amount: [defaults.amount, [planningDebtAmountValidator]],
        installments: [defaults.installments, [Validators.required, Validators.min(1)]],
        firstPaymentDate: [defaultPaymentDate, [Validators.required]],
        currency: [defaults.currency, [isoCurrencyCodeValidator]],
      });
    } else {
      this.planningForm.patchValue({
        amount: defaults.amount,
        installments: defaults.installments,
        firstPaymentDate: defaultPaymentDate,
        currency: defaults.currency,
      });
    }
  }

  onAddDebtSubmit(event: SubmitEvent): void {
    event.preventDefault();
    if (this.planningForm == null || this.creating) return;
    if (this.planningForm.invalid) {
      this.planningForm.markAllAsTouched();
      return;
    }
    void this.addDebt();
  }

  onSimulateSubmit(event: SubmitEvent): void {
    event.preventDefault();
    if (this.creating || this.planningForm == null) return;
    if (this.simulationForm.invalid) {
      this.simulationForm.markAllAsTouched();
      return;
    }
    void this.createJob();
  }

  get jobs() {
    return this.jobsPage.content;
  }

  get canGoPreviousPage() {
    return !this.jobsPage.first;
  }

  get canGoNextPage() {
    return !this.jobsPage.last;
  }

  async createJob() {
    if (this.creating || this.planningForm == null || this.simulationForm.invalid) return;

    this.creating = true;
    this.createError = false;
    this.waitingAsyncResult = false;
    this.lastCreateCompletedFastPath = false;
    this.setPlanningFormBusyDisabled(true);
    this.setSimulationFormBusyDisabled(true);

    try {
      const { horizonMonths } = this.simulationForm.getRawValue() as { horizonMonths: number };
      const v = this.planningForm.getRawValue() as {
        amount: number | null;
        installments: number;
        firstPaymentDate: Date;
        currency: string;
      };
      const pendingDebt: PlanningDebtInputForm = {
        amount: v.amount,
        installments: v.installments,
        firstPaymentDate: dayjs(v.firstPaymentDate),
        currency: v.currency,
      };

      const requestPayload = buildPlanningSimulationRequestPayload({
        horizonMonths,
        debts: this.debts,
        pendingDebt,
      });

      const body: CreateSimulationJobRequestDto = {
        type: SimulationJobType__Obj.PLANNING_SIMULATION,
        requestPayload: JSON.stringify(requestPayload),
      };

      const created = await this.simulationJobService.createJob(body, this.workspace.groupId ?? undefined);
      this.lastCreatedJobId = created.id;
      await this.loadPage(0);

      const fastPath = await this.waitForTerminalStatus(created.id, 5000);

      if (fastPath != null) {
        this.lastCreateCompletedFastPath = true;
        this.mergeJob(fastPath);
      } else {
        this.waitingAsyncResult = true;
      }
    } catch {
      this.createError = true;
      this.lastCreatedJobId = null;
      this.waitingAsyncResult = false;
    } finally {
      this.setPlanningFormBusyDisabled(false);
      this.setSimulationFormBusyDisabled(false);
      this.creating = false;
    }
  }

  async addDebt(): Promise<void> {
    if (this.planningForm == null || this.planningForm.invalid) return;

    const debt = this.planningForm.getRawValue() as {
      amount: number | null;
      installments: number;
      firstPaymentDate: Date;
      currency: string;
    };
    const amount = Number(debt.amount ?? 0);

    this.debts = [
      ...this.debts,
      {
        amount,
        installments: Math.max(1, debt.installments || 1),
        firstPaymentDate: dayjs(debt.firstPaymentDate),
        currency: debt.currency.trim().toUpperCase(),
      },
    ];

    this.syncSimulationHorizonWithDebts({ resetToSuggested: true });
    return this.createDefaultPlanningDebtForm();
  }

  removeDebt(index: number) {
    this.debts = this.debts.filter((_, i) => i !== index);
    this.syncSimulationHorizonWithDebts({ resetToSuggested: false });
  }

  async refreshPage() {
    await this.loadPage(this.jobsPage.number ?? 0);
  }

  async previousPage() {
    if (!this.canGoPreviousPage || this.loading) return;
    await this.loadPage((this.jobsPage.number ?? 0) - 1);
  }

  async nextPage() {
    if (!this.canGoNextPage || this.loading) return;
    await this.loadPage((this.jobsPage.number ?? 0) + 1);
  }

  canNewSimulation(): boolean {
    if (this.workspace.scope !== 'group' || this.workspace.groupId == null) {
      return true;
    }
    return this.groupWithRole?.permissions?.includes(GroupPermissions__Obj.NEW_SIMULATION) === true;
  }

  canDeleteSimulation(job: SimulationJobDto): boolean {
    if (this.workspace.scope !== 'group' || this.workspace.groupId == null) {
      const uid = this.userService.user()?.id;
      return uid != null && job.requestedByUserId === uid;
    }
    return this.groupWithRole?.permissions?.includes(GroupPermissions__Obj.DELETE_SIMULATIONS) === true;
  }

  showCancelSimulation(job: SimulationJobDto): boolean {
    if (!this.canCancel(job)) return false;
    if (this.workspace.scope === 'group' && this.workspace.groupId != null) {
      return this.canNewSimulation();
    }
    return true;
  }

  isRemovingSimulation(jobId: string): boolean {
    return this.removingSimulation.has(jobId);
  }

  confirmDeleteSimulation(job: SimulationJobDto): void {
    this.confirmationService.confirm({
      header: this.translateService.instant('financesPage.simulationJobs.deleteConfirmTitle'),
      message: this.translateService.instant('financesPage.simulationJobs.deleteConfirmMessage'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-secondary',
      accept: () => void this.executeDeleteSimulation(job),
    });
  }

  private async executeDeleteSimulation(job: SimulationJobDto): Promise<void> {
    if (this.removingSimulation.has(job.id)) return;
    this.deleteError = false;
    this.removingSimulation.add(job.id);
    try {
      await this.simulationJobService.deleteJob(job.id, this.workspace.groupId ?? undefined);
      await this.loadPage(this.jobsPage.number ?? 0);
    } catch {
      this.deleteError = true;
    } finally {
      this.removingSimulation.delete(job.id);
    }
  }

  canCancel(job: SimulationJobDto): boolean {
    return job.status === 'QUEUED' || job.status === 'RUNNING';
  }

  isCancelling(jobId: string): boolean {
    return this.cancelling.has(jobId);
  }

  async cancel(job: SimulationJobDto) {
    if (!this.canCancel(job) || this.isCancelling(job.id)) return;
    this.cancelling.add(job.id);
    try {
      const updated = await this.simulationJobService.cancelJob(job.id, this.workspace.groupId ?? undefined);
      this.mergeJob(updated);
    } finally {
      this.cancelling.delete(job.id);
    }
  }

  statusKey(status: SimulationJobStatus): string {
    return `financesPage.simulationJobs.status.${status}`;
  }

  statusSeverity(status: SimulationJobStatus): 'success' | 'danger' | 'warn' | 'info' | 'secondary' {
    switch (status) {
      case 'COMPLETED':
        return 'success';
      case 'FAILED':
        return 'danger';
      case 'CANCELLED':
        return 'secondary';
      case 'RUNNING':
        return 'info';
      default:
        return 'warn';
    }
  }

  outcomeBandKey(outcomeBand: PlanningSimulationOutcomeBand): string {
    return `financesPage.simulationJobs.outcomeBand.${outcomeBand}`;
  }

  outcomeBandSeverity(outcomeBand: PlanningSimulationOutcomeBand): 'success' | 'danger' | 'warn' | 'info' {
    switch (outcomeBand) {
      case 'FITS':
        return 'success';
      case 'FITS_BUT_CANNOT_SUSTAIN_SCHEDULED_GOAL_CONTRIBUTIONS':
        return 'warn';
      case 'FITS_IF_GOAL_ALLOCATIONS_ARE_REDUCED':
        return 'info';
      case 'DOES_NOT_FIT':
        return 'danger';
    }
  }

  parsedResult(job: SimulationJobDto): PlanningSimulationResultPayload | null {
    if (this.parsedResultsByJobId.has(job.id)) {
      return this.parsedResultsByJobId.get(job.id) ?? null;
    }

    const parsed = parsePlanningSimulationResultPayload(job.resultPayload);
    this.parsedResultsByJobId.set(job.id, parsed);
    return parsed;
  }

  shouldShowRawResult(job: SimulationJobDto): boolean {
    return job.resultPayload != null && this.parsedResult(job) == null;
  }

  isLatestCreated(job: SimulationJobDto): boolean {
    return job.id === this.lastCreatedJobId;
  }

  currencyPoint(row: { byCurrency: Record<string, PlanningSimulationCurrencyPoint> }, currency: string): PlanningSimulationCurrencyPoint {
    return row.byCurrency[currency];
  }

  currenciesForTimelineRow(row: { byCurrency: Record<string, PlanningSimulationCurrencyPoint> }): string[] {
    return Object.keys(row.byCurrency).sort();
  }

  private async loadPage(page: number) {
    if (page < 0) return;
    this.loading = true;
    this.loadError = false;
    try {
      this.jobsPage = await this.simulationJobService.listJobs(
        {
          page,
          size: this.pageSize,
          sort: [{ property: 'createdAt', direction: 'DESC' }],
        },
        this.workspace.groupId ?? undefined,
      );
      this.parsedResultsByJobId.clear();
    } catch {
      this.loadError = true;
    } finally {
      this.loading = false;
    }
  }

  private async refreshJob(jobId: string) {
    const idx = this.jobs.findIndex(j => j.id === jobId);
    if (idx < 0) {
      return;
    }

    try {
      const fresh = await this.simulationJobService.getJob(jobId, this.workspace.groupId ?? undefined);
      this.mergeJob(fresh);
      if (this.lastCreatedJobId === fresh.id && this.isTerminalStatus(fresh.status)) {
        this.waitingAsyncResult = false;
      }
    } catch {
      // ignore transient errors while syncing SSE updates
    }
  }

  private mergeJob(updated: SimulationJobDto) {
    const idx = this.jobs.findIndex(j => j.id === updated.id);
    if (idx < 0) {
      return;
    }
    const content = [...this.jobs];
    content[idx] = updated;
    this.jobsPage = { ...this.jobsPage, content };
    this.parsedResultsByJobId.delete(updated.id);
    if (this.lastCreatedJobId === updated.id && this.isTerminalStatus(updated.status)) {
      this.waitingAsyncResult = false;
    }
  }

  private isTerminalStatus(status: SimulationJobStatus): boolean {
    return status === 'COMPLETED' || status === 'FAILED' || status === 'CANCELLED';
  }

  private async waitForTerminalStatus(jobId: string, timeoutMs: number): Promise<SimulationJobDto | null> {
    const start = Date.now();
    while (Date.now() - start < timeoutMs) {
      try {
        const job = await this.simulationJobService.getJob(jobId, this.workspace.groupId ?? undefined);
        if (this.isTerminalStatus(job.status)) {
          return job;
        }
      } catch {
        // ignore transient polling errors while worker processes the job
      }
      await this.sleep(350);
    }
    return null;
  }

  private async sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /** Installments and first payment date disabled while a job is being created. */
  private setPlanningFormBusyDisabled(disabled: boolean) {
    if (this.planningForm == null) return;
    for (const name of ['installments', 'firstPaymentDate'] as const) {
      const c = this.planningForm.get(name);
      if (c == null) continue;
      if (disabled) {
        c.disable({ emitEvent: false });
      } else {
        c.enable({ emitEvent: false });
      }
    }
  }

  private setSimulationFormBusyDisabled(disabled: boolean) {
    const c = this.simulationForm.get('horizonMonths');
    if (c == null) return;
    if (disabled) {
      c.disable({ emitEvent: false });
    } else {
      c.enable({ emitEvent: false });
    }
  }

  /**
   * Mínimo do horizonte = maior parcela entre dívidas (até 480).
   * Após incluir dívida, o valor sugerido é sempre esse mínimo + 1 (ex.: máx 8 → 9).
   * Após remover, só ajusta o valor se ficar abaixo do novo mínimo ou acima de 480.
   */
  private syncSimulationHorizonWithDebts(options: { resetToSuggested: boolean }): void {
    const maxInst = this.debts.length === 0 ? 1 : Math.max(...this.debts.map(d => Math.max(1, d.installments || 1)));
    const minH = Math.min(Math.max(1, maxInst), 480);
    const suggested = Math.min(maxInst + 1, 480);

    const ctrl = this.simulationForm.get('horizonMonths');
    if (ctrl == null) return;

    this.simulationHorizonMin = minH;
    ctrl.setValidators([Validators.required, Validators.min(minH), Validators.max(480)]);
    ctrl.updateValueAndValidity({ emitEvent: false });

    const cur = ctrl.value;
    const n = typeof cur === 'number' ? cur : Number(cur);
    const curNum = Number.isFinite(n) ? n : NaN;
    if (options.resetToSuggested || cur == null || !Number.isFinite(curNum) || curNum < minH || curNum > 480) {
      ctrl.patchValue(suggested, { emitEvent: false });
      ctrl.updateValueAndValidity({ emitEvent: false });
    }
  }
}

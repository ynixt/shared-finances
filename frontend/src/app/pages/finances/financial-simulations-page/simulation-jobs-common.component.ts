import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowsRotate, faBan, faFlask } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { filter } from 'rxjs';

import { ButtonDirective } from 'primeng/button';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';

import { SimulationJobDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/simulationjobs';
import { SimulationJobStatusEventDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/simulationjobs';
import { SimulationJobStatus } from '../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { Page } from '../../../models/pagination';
import { LocalDatePipe } from '../../../pipes/local-date.pipe';
import { createEmptyPage } from '../../../services/pagination.service';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import { resolveGoalWorkspaceContext } from '../financial-goals-page/goal-workspace-context';
import { SimulationJobService } from '../services/simulation-job.service';
import { UserActionEventService } from '../services/user-action-event.service';

@UntilDestroy()
@Component({
  selector: 'app-simulation-jobs-common',
  imports: [FinancesTitleBarComponent, TranslatePipe, ButtonDirective, Tag, Tooltip, FaIconComponent, LocalDatePipe, Message],
  templateUrl: './simulation-jobs-common.component.html',
})
export class SimulationJobsCommonComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly simulationJobService = inject(SimulationJobService);
  private readonly userActionEventService = inject(UserActionEventService);

  readonly createIcon = faFlask;
  readonly refreshIcon = faArrowsRotate;
  readonly cancelIcon = faBan;
  readonly pageSize = 12;
  readonly workspace = resolveGoalWorkspaceContext(this.route);
  readonly closeRouterLink = this.workspace.goalsRoot;

  jobsPage: Page<SimulationJobDto> = createEmptyPage();
  loading = false;
  loadError = false;
  creating = false;
  cancelling = new Set<string>();

  constructor() {
    void this.loadPage(0);

    if (this.workspace.scope === 'group' && this.workspace.groupId != null) {
      this.userActionEventService.groupEvents$
        .pipe(
          filter(event => event.event === 'SIMULATION_JOB' && event.groupId === this.workspace.groupId),
          untilDestroyed(this),
        )
        .subscribe(event => {
          const payload = event.data as SimulationJobStatusEventDto;
          void this.refreshJob(payload.id);
        });
    } else {
      this.userActionEventService.simulationJobUpdates$.pipe(untilDestroyed(this)).subscribe(event => {
        void this.refreshJob(event.id);
      });
    }

    this.userActionEventService.resyncRequired$.pipe(untilDestroyed(this)).subscribe(() => {
      void this.loadPage(this.jobsPage.number ?? 0);
    });
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
    if (this.creating) return;
    this.creating = true;
    try {
      await this.simulationJobService.createJob(undefined, this.workspace.groupId ?? undefined);
      await this.loadPage(0);
    } finally {
      this.creating = false;
    }
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
    return `financesPage.goalsPage.simulationJobs.status.${status}`;
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
  }
}

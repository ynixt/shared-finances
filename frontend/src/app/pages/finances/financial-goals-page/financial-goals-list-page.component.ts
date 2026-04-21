import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowUpRightFromSquare, faPenToSquare } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonDirective } from 'primeng/button';
import { Card } from 'primeng/card';
import { DataView, DataViewLazyLoadEvent } from 'primeng/dataview';
import { Tooltip } from 'primeng/tooltip';

import { FinancialGoalSummaryDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/goals';
import { GroupPermissions__Obj } from '../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { Page } from '../../../models/pagination';
import { LocalDatePipe } from '../../../pipes/local-date.pipe';
import { createEmptyPage } from '../../../services/pagination.service';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import { FinancialGoalService } from '../services/financial-goal.service';
import { GroupService } from '../services/group.service';
import { resolveGoalWorkspaceContext } from './goal-workspace-context';

@Component({
  selector: 'app-financial-goals-list-page',
  imports: [FinancesTitleBarComponent, TranslatePipe, RouterLink, ButtonDirective, DataView, Card, FaIconComponent, Tooltip, LocalDatePipe],
  templateUrl: './financial-goals-list-page.component.html',
})
export class FinancialGoalsListPageComponent {
  private readonly route = inject(ActivatedRoute);
  readonly editFinanceIcon = faPenToSquare;
  readonly openFinanceIcon = faArrowUpRightFromSquare;
  readonly pageSize = 12;
  readonly workspace = resolveGoalWorkspaceContext(this.route);
  private loadedPage: number | null = null;
  private loadingPage: number | null = null;

  goals: Page<FinancialGoalSummaryDto> = createEmptyPage();
  loading = true;
  loadError = false;
  canManageGoals = true;

  constructor(
    private financialGoalService: FinancialGoalService,
    private groupService: GroupService,
  ) {
    void this.loadWorkspaceData();
  }

  get newGoalRouterLink(): string[] {
    return [...this.workspace.goalsRoot, 'new'];
  }

  get closeRouterLink(): string[] | string {
    if (this.workspace.scope === 'group' && this.workspace.groupId != null) {
      return ['/app/groups', this.workspace.groupId];
    }
    return '..';
  }

  goalDetailRouterLink(goalId: string): string[] {
    return [...this.workspace.goalsRoot, goalId];
  }

  goalEditRouterLink(goalId: string): string[] {
    return [...this.workspace.goalsRoot, goalId, 'edit'];
  }

  onLazyLoad(event: DataViewLazyLoadEvent) {
    const first = event.first ?? 0;
    const newPage = Math.floor(first / this.pageSize);
    if (this.loadingPage === newPage) {
      return;
    }
    if (this.loadedPage === newPage && !this.loadError) {
      return;
    }
    void this.loadPage(newPage);
  }

  private async loadPage(page = 0) {
    if (this.loadingPage === page) {
      return;
    }
    this.loadingPage = page;
    this.loading = true;
    this.loadError = false;
    try {
      if (this.workspace.scope === 'group' && this.workspace.groupId != null) {
        this.goals = await this.financialGoalService.listGroupGoals(this.workspace.groupId, {
          page,
          size: this.pageSize,
          sort: [{ property: 'name', direction: 'ASC' }],
        });
      } else {
        this.goals = await this.financialGoalService.listIndividualGoals({
          page,
          size: this.pageSize,
          sort: [{ property: 'name', direction: 'ASC' }],
        });
      }
      this.loadedPage = page;
    } catch {
      this.loadError = true;
    } finally {
      this.loading = false;
      this.loadingPage = null;
    }
  }

  private async loadWorkspaceData() {
    await this.resolveManageGoalsPermission();
    await this.loadPage();
  }

  private async resolveManageGoalsPermission() {
    if (this.workspace.scope !== 'group' || this.workspace.groupId == null) {
      this.canManageGoals = true;
      return;
    }

    try {
      const group = await this.groupService.getGroup(this.workspace.groupId);
      this.canManageGoals = group.permissions.includes(GroupPermissions__Obj.MANAGE_GOALS);
    } catch {
      this.canManageGoals = false;
    }
  }
}

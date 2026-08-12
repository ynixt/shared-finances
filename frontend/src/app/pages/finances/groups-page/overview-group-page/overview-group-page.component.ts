import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { faArrowRightFromBracket, faPencil, faPeopleGroup, faScaleBalanced } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { Subject, debounceTime, filter } from 'rxjs';

import dayjs from 'dayjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { Message } from 'primeng/message';
import { ProgressSpinner } from 'primeng/progressspinner';

import { GroupOverviewDashboardDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/dashboard';
import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { EventForListDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { GroupPermissions__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { WalletItemType__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums/wallet-item-type';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { PlanEntitlementsStore } from '../../../../services/plan-entitlements.store';
import { UserService } from '../../../../services/user.service';
import { DashboardFeedFilters, EMPTY_DASHBOARD_FEED_FILTERS } from '../../components/dashboard-filters/dashboard-feed-filters.model';
import { GroupDashboardFiltersComponent } from '../../components/dashboard-filters/group-dashboard-filters.component';
import { FinancesTitleBarComponent, FinancesTitleBarExtraButton } from '../../components/finances-title-bar/finances-title-bar.component';
import { GroupDebtPairsPanelComponent } from '../../components/group-debt-pairs-panel/group-debt-pairs-panel.component';
import { GroupOverviewDashboardChartsComponent } from '../../components/group-overview-dashboard-charts/group-overview-dashboard-charts.component';
import { GroupUserListComponent } from '../../components/group-user-list/group-user-list.component';
import { OverviewDashboardCardsComponent } from '../../components/overview-dashboard-cards/overview-dashboard-cards.component';
import {
  AdvancedDatePickerComponent,
  DateRange,
} from '../../components/wallet-entry-table/components/advanced-date-picker/advanced-date-picker.component';
import { WalletEntryTableComponent } from '../../components/wallet-entry-table/wallet-entry-table.component';
import {
  MONTH_QUERY_PARAM_FORMAT,
  createMonthDateRange,
  readDateRangeFromQueryParams,
  syncDateQueryParams,
} from '../../services/date-query-params.util';
import { GroupService } from '../../services/group.service';
import { GroupsActionEventService } from '../../services/groups-action-event.service';
import { OverviewDashboardService } from '../../services/overview-dashboard.service';
import { UserActionEventService } from '../../services/user-action-event.service';
import { shouldRefreshGroupDashboardForEvent } from './group-dashboard-event-filter';
import { GroupDashboardFilterOptionsService } from './group-dashboard-filter-options.service';

@Component({
  selector: 'app-overview-group-page',
  imports: [
    ProgressSpinner,
    FinancesTitleBarComponent,
    TranslatePipe,
    GroupUserListComponent,
    ReactiveFormsModule,
    AdvancedDatePickerComponent,
    Message,
    OverviewDashboardCardsComponent,
    GroupOverviewDashboardChartsComponent,
    GroupDebtPairsPanelComponent,
    GroupDashboardFiltersComponent,
    WalletEntryTableComponent,
    ConfirmDialog,
  ],
  templateUrl: './overview-group-page.component.html',
  styleUrl: './overview-group-page.component.scss',
  providers: [ConfirmationService, GroupDashboardFilterOptionsService],
})
@UntilDestroy()
export class OverviewGroupPageComponent {
  readonly dateControl = new FormControl<DateRange | undefined>(undefined);
  private readonly refreshBurst$ = new Subject<void>();
  private readonly debtsButtonIcon = faScaleBalanced;

  groupId: string | undefined;
  group: GroupWithRoleDto | null = null;
  dashboard: GroupOverviewDashboardDto | undefined;
  loading = true;
  dashboardLoading = false;
  submitting = false;
  dashboardFilters: DashboardFeedFilters = { ...EMPTY_DASHBOARD_FEED_FILTERS };
  tableRefreshKey = 0;

  extraButtons: FinancesTitleBarExtraButton[] = this.createExtraButtons();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private groupService: GroupService,
    private filterOptions: GroupDashboardFilterOptionsService,
    private groupsActionEventService: GroupsActionEventService,
    private overviewDashboardService: OverviewDashboardService,
    private userActionEventService: UserActionEventService,
    private messageService: MessageService,
    private errorMessageService: ErrorMessageService,
    private confirmationService: ConfirmationService,
    private userService: UserService,
    private translateService: TranslateService,
    protected readonly planEntitlements: PlanEntitlementsStore,
  ) {
    const initialDateRange =
      readDateRangeFromQueryParams(this.route.snapshot.queryParamMap, 'normal') ?? createMonthDateRange(dayjs(), 'normal');
    this.dateControl.setValue(initialDateRange, { emitEvent: false });

    this.route.paramMap.pipe(untilDestroyed(this)).subscribe(params => {
      const id = params.get('id');
      if (id == null) {
        void this.goToNotFound();
        return;
      }

      this.groupId = id;
      void this.loadGroupAndDashboard(id);
    });

    this.dateControl.valueChanges.pipe(untilDestroyed(this)).subscribe(() => this.applyDateRange(true));
    this.refreshBurst$.pipe(debounceTime(250), untilDestroyed(this)).subscribe(() => {
      void this.refreshDashboardAndFeed();
    });
    this.userActionEventService.transactionInserted$.pipe(untilDestroyed(this)).subscribe(event => this.handleTransactionEvent(event));
    this.userActionEventService.transactionUpdated$.pipe(untilDestroyed(this)).subscribe(event => this.handleTransactionEvent(event));
    this.userActionEventService.transactionDeleted$.pipe(untilDestroyed(this)).subscribe(event => this.handleTransactionEvent(event));
    this.userActionEventService.resyncRequired$.pipe(untilDestroyed(this)).subscribe(() => {
      void this.refreshDashboardAndFeed();
    });
  }

  readonly memberOptionsGetter = (page = 0, query?: string) =>
    this.groupId == null ? Promise.resolve([]) : this.filterOptions.loadMembers(this.groupId, page, query);
  readonly bankAccountOptionsGetter = (page = 0, query?: string) =>
    this.groupId == null
      ? Promise.resolve([])
      : this.filterOptions.loadWalletItems(this.groupId, WalletItemType__Obj.BANK_ACCOUNT, page, query);
  readonly creditCardOptionsGetter = (page = 0, query?: string) =>
    this.groupId == null
      ? Promise.resolve([])
      : this.filterOptions.loadWalletItems(this.groupId, WalletItemType__Obj.CREDIT_CARD, page, query);
  readonly categoryOptionsGetter = (page = 0, query?: string) =>
    this.groupId == null ? Promise.resolve([]) : this.filterOptions.loadCategories(this.groupId, page, query);

  onFiltersChange(filters: DashboardFeedFilters) {
    this.dashboardFilters = filters;
    this.bumpTableRefreshKey();
  }

  private async loadGroupAndDashboard(groupId: string) {
    this.loading = true;

    try {
      this.group = await this.groupService.getGroup(groupId);
      this.extraButtons = this.createExtraButtons();
      this.trackGroup(groupId);
      await this.applyDateRange(false);
    } catch (error) {
      if (error instanceof HttpErrorResponse && (error.status === 404 || error.status === 400)) {
        await this.goToNotFound();
        return;
      }

      this.errorMessageService.handleError(error, this.messageService);
      throw error;
    } finally {
      this.loading = false;
    }
  }

  private async applyDateRange(syncUrl: boolean): Promise<void> {
    const dateRange = this.dateControl.value ?? undefined;
    if (syncUrl) {
      await syncDateQueryParams(this.route, this.router, dateRange, 'normal');
    }
    await this.loadDashboard();
  }

  private async loadDashboard() {
    if (this.groupId == null) {
      this.dashboard = undefined;
      return;
    }

    const dateRange = this.dateControl.value;
    if (dateRange == null) {
      this.dashboard = undefined;
      return;
    }

    const month = dateRange.startDate.format(MONTH_QUERY_PARAM_FORMAT);
    this.dashboardLoading = true;

    try {
      this.dashboard = await this.overviewDashboardService.getGroupOverview(this.groupId, month);
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 400) {
        this.dashboard = undefined;
      }
      this.errorMessageService.handleError(error, this.messageService);
    } finally {
      this.dashboardLoading = false;
    }
  }

  private async refreshDashboardAndFeed() {
    await this.loadDashboard();
    this.bumpTableRefreshKey();
  }

  private handleTransactionEvent(event: EventForListDto) {
    if (!shouldRefreshGroupDashboardForEvent(event, this.groupId, this.dateControl.value, this.dashboardFilters)) {
      return;
    }
    this.refreshBurst$.next();
  }

  private bumpTableRefreshKey() {
    this.tableRefreshKey += 1;
  }

  private goToNotFound() {
    return this.router.navigateByUrl('/not-found');
  }

  private createExtraButtons(): FinancesTitleBarExtraButton[] {
    const extraButtons: FinancesTitleBarExtraButton[] = [];
    extraButtons.push({
      routerLink: 'team',
      rounded: true,
      tooltip: 'financesPage.groupsPage.overviewPage.manageTeam',
      icon: faPeopleGroup,
    });
    extraButtons.push({
      routerLink: 'debts',
      rounded: true,
      tooltip: 'financesPage.groupsPage.overviewPage.viewDebts',
      icon: this.debtsButtonIcon,
    });
    if (this.group?.permissions?.includes(GroupPermissions__Obj.EDIT_GROUP)) {
      extraButtons.push({
        routerLink: 'edit',
        rounded: true,
        tooltip: 'general.edit',
        icon: faPencil,
      });
    }
    if (this.group != null && !this.group.isOwner) {
      extraButtons.push({
        click: () => this.askForConfirmationToLeave(),
        rounded: true,
        tooltip: 'financesPage.groupsPage.overviewPage.leave.tooltip',
        icon: faArrowRightFromBracket,
        severity: 'danger',
        text: false,
      });
    }
    return extraButtons;
  }

  private trackGroup(groupId: string) {
    this.groupsActionEventService.groupUpdated$
      .pipe(
        untilDestroyed(this),
        filter(e => e.groupId == groupId),
      )
      .subscribe(e => this.groupUpdated(e.data));

    this.groupsActionEventService.ownershipChanged$
      .pipe(
        untilDestroyed(this),
        filter(e => e.data.groupId == groupId),
      )
      .subscribe(() => void this.ownershipChanged());

    this.groupsActionEventService.memberLeft$
      .pipe(
        untilDestroyed(this),
        filter(e => e.data.groupId == groupId && e.data.userId === this.userService.user()?.id),
      )
      .subscribe(() => this.groupDeleted());

    this.groupsActionEventService.groupDeleted$
      .pipe(
        untilDestroyed(this),
        filter(e => e.groupId == groupId),
      )
      .subscribe(() => this.groupDeleted());
  }

  private groupUpdated(newGroup: { id: string; name: string }) {
    if (this.group != null) this.group = { ...this.group, name: newGroup.name };
    this.extraButtons = this.createExtraButtons();
  }

  private async ownershipChanged() {
    if (this.group == null || this.groupId == null) return;
    this.group = await this.groupService.getGroup(this.groupId);
    this.extraButtons = this.createExtraButtons();
  }

  private askForConfirmationToLeave() {
    if (this.group == null || this.group.isOwner) return;
    const balance = this.currentUserOutstandingBalance();
    const balanceWarning =
      balance > 0
        ? ' ' +
          this.translateMessage('financesPage.groupsPage.overviewPage.leave.balanceWarning', {
            amount: `${balance.toFixed(2)} ${this.dashboard?.currency ?? ''}`.trim(),
          })
        : '';
    this.confirmationService.confirm({
      header: this.translateMessage('financesPage.groupsPage.overviewPage.leave.title'),
      message: this.translateMessage('financesPage.groupsPage.overviewPage.leave.confirmation', { name: this.group.name }) + balanceWarning,
      acceptLabel: this.translateMessage('financesPage.groupsPage.overviewPage.leave.action'),
      rejectLabel: this.translateMessage('general.cancel'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => void this.leaveGroup(),
    });
  }

  private currentUserOutstandingBalance(): number {
    const userId = this.userService.user()?.id;
    if (userId == null) return 0;
    return (this.dashboard?.debtPairs ?? [])
      .filter(pair => pair.payerId === userId || pair.receiverId === userId)
      .reduce((total, pair) => total + Number(pair.outstandingAmount), 0);
  }

  private async leaveGroup() {
    if (this.groupId == null) return;
    this.submitting = true;
    try {
      await this.groupService.leaveGroup(this.groupId);
      await this.router.navigate(['/app']);
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
    } finally {
      this.submitting = false;
    }
  }

  private translateMessage(key: string, params?: Record<string, unknown>): string {
    return this.translateService.instant(key, params);
  }

  private groupDeleted() {
    void this.router.navigate(['/app']);
  }
}

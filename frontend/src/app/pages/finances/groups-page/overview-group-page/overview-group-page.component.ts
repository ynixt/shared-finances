import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { faPencil, faPeopleGroup, faScaleBalanced } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { Subject, debounceTime, filter } from 'rxjs';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { Message } from 'primeng/message';
import { ProgressSpinner } from 'primeng/progressspinner';

import { GroupOverviewDashboardDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/dashboard';
import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { GroupUserDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups/group-user-dto';
import { WalletItemSearchResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { EventForListDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { GroupPermissions__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { WalletItemType } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums/wallet-item-type';
import { WalletItemType__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums/wallet-item-type';
import { ErrorMessageService } from '../../../../services/error-message.service';
import {
  DashboardFeedFilters,
  DashboardFilterOption,
  EMPTY_DASHBOARD_FEED_FILTERS,
} from '../../components/dashboard-filters/dashboard-feed-filters.model';
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
import { GroupCategoriesService } from '../../services/group-categories.service';
import { GroupWalletItemService } from '../../services/group-wallet-item.service';
import { GroupService } from '../../services/group.service';
import { GroupsActionEventService } from '../../services/groups-action-event.service';
import { OverviewDashboardService } from '../../services/overview-dashboard.service';
import { UserActionEventService } from '../../services/user-action-event.service';

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
  ],
  templateUrl: './overview-group-page.component.html',
  styleUrl: './overview-group-page.component.scss',
})
@UntilDestroy()
export class OverviewGroupPageComponent {
  readonly dateControl = new FormControl<DateRange | undefined>(undefined);
  private readonly dashboardFilterPageSize = 10;
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
    private groupWalletItemService: GroupWalletItemService,
    private groupCategoriesService: GroupCategoriesService,
    private groupsActionEventService: GroupsActionEventService,
    private overviewDashboardService: OverviewDashboardService,
    private userActionEventService: UserActionEventService,
    private messageService: MessageService,
    private errorMessageService: ErrorMessageService,
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

  readonly memberOptionsGetter = this.loadMembers.bind(this);
  readonly bankAccountOptionsGetter = this.loadBankAccounts.bind(this);
  readonly creditCardOptionsGetter = this.loadCreditCards.bind(this);
  readonly categoryOptionsGetter = this.loadCategories.bind(this);

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
    if (!this.shouldRefreshForEvent(event)) {
      return;
    }
    this.refreshBurst$.next();
  }

  private shouldRefreshForEvent(event: EventForListDto): boolean {
    if (this.groupId == null || event.group?.id !== this.groupId) {
      return false;
    }

    const dateRange = this.dateControl.value;
    if (dateRange == null) {
      return false;
    }

    const eventDate = dayjs(event.date);
    const inRange =
      (dateRange.startDate.isBefore(eventDate) || dateRange.startDate.isSame(eventDate, 'day')) &&
      (dateRange.endDate == null || dateRange.endDate.isAfter(eventDate) || dateRange.endDate.isSame(eventDate, 'day'));
    if (!inRange) {
      return false;
    }

    if (this.dashboardFilters.entryTypes.length > 0 && !this.dashboardFilters.entryTypes.includes(event.type)) {
      return false;
    }
    if (
      this.dashboardFilters.bankAccountIds.length > 0 &&
      event.entries.find(entry => this.dashboardFilters.bankAccountIds.includes(entry.walletItemId)) == null
    ) {
      return false;
    }
    if (
      this.dashboardFilters.creditCardIds.length > 0 &&
      event.entries.find(entry => this.dashboardFilters.creditCardIds.includes(entry.walletItemId)) == null
    ) {
      return false;
    }
    if (
      this.dashboardFilters.memberIds.length > 0 &&
      event.entries.find(
        entry => entry.walletItem.user?.id != null && this.dashboardFilters.memberIds.includes(entry.walletItem.user.id),
      ) == null &&
      (event.user?.id == null || !this.dashboardFilters.memberIds.includes(event.user.id))
    ) {
      return false;
    }

    if (!this.matchesCategoryFilters(event)) {
      return false;
    }

    return true;
  }

  private matchesCategoryFilters(event: EventForListDto): boolean {
    if (this.dashboardFilters.categoryIds.length === 0 && !this.dashboardFilters.includeUncategorized) {
      return true;
    }

    const categoryId = event.category?.id;
    if (categoryId == null) {
      return this.dashboardFilters.includeUncategorized;
    }

    return this.dashboardFilters.categoryIds.length === 0 || this.dashboardFilters.categoryIds.includes(categoryId);
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
    return extraButtons;
  }

  private trackGroup(groupId: string) {
    this.groupsActionEventService.groupUpdated$
      .pipe(
        untilDestroyed(this),
        filter(e => e.groupId == groupId),
      )
      .subscribe(e => this.groupUpdated(e.data));

    this.groupsActionEventService.groupDeleted$
      .pipe(
        untilDestroyed(this),
        filter(e => e.groupId == groupId),
      )
      .subscribe(() => this.groupDeleted());
  }

  private groupUpdated(newGroup: GroupWithRoleDto) {
    this.group = newGroup;
    this.extraButtons = this.createExtraButtons();
  }

  private groupDeleted() {
    void this.router.navigate(['/app']);
  }

  private async loadMembers(page = 0, query?: string): Promise<DashboardFilterOption[]> {
    if (this.groupId == null) return [];
    const members = await this.groupService.findAllMembers(this.groupId);
    return this.paginateAndMapMembers(members, page, query);
  }

  private async loadBankAccounts(page = 0, query?: string): Promise<DashboardFilterOption[]> {
    return this.loadWalletItemsByType(WalletItemType__Obj.BANK_ACCOUNT, page, query);
  }

  private async loadCreditCards(page = 0, query?: string): Promise<DashboardFilterOption[]> {
    return this.loadWalletItemsByType(WalletItemType__Obj.CREDIT_CARD, page, query);
  }

  private async loadWalletItemsByType(type: WalletItemType, page = 0, query?: string): Promise<DashboardFilterOption[]> {
    if (this.groupId == null) return [];
    const pageData = await this.groupWalletItemService.getAllItems(this.groupId, { page: 0, size: 500, sort: 'name' });
    const filtered = pageData.content.filter(item => item.type === type);
    const queried = this.filterByQuery(filtered, query, item => item.name);
    return this.paginate(queried, page).map(item => ({ id: item.id, label: item.name }));
  }

  private async loadCategories(page = 0, query?: string): Promise<DashboardFilterOption[]> {
    if (this.groupId == null) return [];
    const categories = await this.groupCategoriesService.getAllCategories(
      this.groupId,
      {
        onlyRoot: false,
        mountChildren: false,
        query,
      },
      {
        page,
        size: this.dashboardFilterPageSize,
        sort: 'name',
      },
    );

    return categories.content.map(category => ({ id: category.id, label: category.name }));
  }

  private paginateAndMapMembers(members: GroupUserDto[], page: number, query?: string): DashboardFilterOption[] {
    const filtered = this.filterByQuery(members, query, member => `${member.user.firstName} ${member.user.lastName}`);
    return this.paginate(filtered, page).map(member => ({
      id: member.user.id,
      label: `${member.user.firstName} ${member.user.lastName}`.trim(),
    }));
  }

  private filterByQuery<T>(items: T[], query: string | undefined, mapLabel: (item: T) => string): T[] {
    const normalizedQuery = query?.trim().toLowerCase();
    if (normalizedQuery == null || normalizedQuery.length === 0) {
      return items;
    }
    return items.filter(item => mapLabel(item).toLowerCase().includes(normalizedQuery));
  }

  private paginate<T>(items: T[], page: number): T[] {
    const start = page * this.dashboardFilterPageSize;
    return items.slice(start, start + this.dashboardFilterPageSize);
  }
}

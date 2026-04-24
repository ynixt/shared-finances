import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { Subject, debounceTime } from 'rxjs';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { Message } from 'primeng/message';
import { ProgressSpinner } from 'primeng/progressspinner';

import { OverviewDashboardDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/dashboard';
import { EventForListDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { ErrorMessageService } from '../../../services/error-message.service';
import { DashboardFeedFilters, EMPTY_DASHBOARD_FEED_FILTERS } from '../components/dashboard-filters/dashboard-feed-filters.model';
import { IndividualDashboardFiltersComponent } from '../components/dashboard-filters/individual-dashboard-filters.component';
import { OverviewDashboardCardsComponent } from '../components/overview-dashboard-cards/overview-dashboard-cards.component';
import { OverviewDashboardChartsComponent } from '../components/overview-dashboard-charts/overview-dashboard-charts.component';
import {
  AdvancedDatePickerComponent,
  DateRange,
} from '../components/wallet-entry-table/components/advanced-date-picker/advanced-date-picker.component';
import { WalletEntryTableComponent } from '../components/wallet-entry-table/wallet-entry-table.component';
import {
  MONTH_QUERY_PARAM_FORMAT,
  createMonthDateRange,
  readDateRangeFromQueryParams,
  syncDateQueryParams,
} from '../services/date-query-params.util';
import { OverviewDashboardService } from '../services/overview-dashboard.service';
import { UserActionEventService } from '../services/user-action-event.service';

@Component({
  selector: 'app-finances-overview-page',
  imports: [
    TranslatePipe,
    ReactiveFormsModule,
    AdvancedDatePickerComponent,
    ProgressSpinner,
    OverviewDashboardChartsComponent,
    OverviewDashboardCardsComponent,
    Message,
    WalletEntryTableComponent,
    IndividualDashboardFiltersComponent,
  ],
  templateUrl: './finances-overview-page.component.html',
})
@UntilDestroy()
export class FinancesOverviewPageComponent {
  readonly dateControl = new FormControl<DateRange | undefined>(undefined);

  loading = false;
  dashboard: OverviewDashboardDto | undefined = undefined;
  dashboardFilters: DashboardFeedFilters = { ...EMPTY_DASHBOARD_FEED_FILTERS };
  tableRefreshKey = 0;

  private readonly refreshBurst$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private overviewDashboardService: OverviewDashboardService,
    private userActionEventService: UserActionEventService,
    private errorMessageService: ErrorMessageService,
    private messageService: MessageService,
  ) {
    const initialDateRange =
      readDateRangeFromQueryParams(this.route.snapshot.queryParamMap, 'normal') ?? createMonthDateRange(dayjs(), 'normal');
    this.dateControl.setValue(initialDateRange, { emitEvent: false });
    this.applyDateRange(false);

    this.dateControl.valueChanges.pipe(untilDestroyed(this)).subscribe(() => this.applyDateRange(true));

    this.refreshBurst$.pipe(debounceTime(250), untilDestroyed(this)).subscribe(() => {
      void this.refreshOverviewAndFeed();
    });
    this.userActionEventService.transactionInserted$.pipe(untilDestroyed(this)).subscribe(event => this.handleTransactionEvent(event));
    this.userActionEventService.transactionUpdated$.pipe(untilDestroyed(this)).subscribe(event => this.handleTransactionEvent(event));
    this.userActionEventService.transactionDeleted$.pipe(untilDestroyed(this)).subscribe(event => this.handleTransactionEvent(event));
    this.userActionEventService.resyncRequired$.pipe(untilDestroyed(this)).subscribe(() => {
      void this.refreshOverviewAndFeed();
    });
  }

  onFiltersChange(filters: DashboardFeedFilters) {
    this.dashboardFilters = filters;
    this.bumpTableRefreshKey();
  }

  private applyDateRange(syncUrl: boolean) {
    const dateRange = this.dateControl.value ?? undefined;
    if (syncUrl) {
      void syncDateQueryParams(this.route, this.router, dateRange, 'normal');
    }

    void this.loadOverview();
  }

  private async refreshOverviewAndFeed() {
    await this.loadOverview();
    this.bumpTableRefreshKey();
  }

  private handleTransactionEvent(event: EventForListDto) {
    if (!this.shouldRefreshForEvent(event)) {
      return;
    }
    this.refreshBurst$.next();
  }

  private shouldRefreshForEvent(event: EventForListDto): boolean {
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

    if (!this.matchesCategoryFilters(event)) {
      return false;
    }

    if (this.dashboardFilters.groupIds.length > 0) {
      return event.group?.id != null && this.dashboardFilters.groupIds.includes(event.group.id);
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

  private async loadOverview() {
    const dateRange = this.dateControl.value;
    if (dateRange == null) {
      this.dashboard = undefined;
      return;
    }

    const month = dateRange.startDate.format(MONTH_QUERY_PARAM_FORMAT);

    this.loading = true;

    try {
      this.dashboard = await this.overviewDashboardService.getOverview(month);
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 400) {
        this.dashboard = undefined;
      }

      this.errorMessageService.handleError(error, this.messageService);
    } finally {
      this.loading = false;
    }
  }
}

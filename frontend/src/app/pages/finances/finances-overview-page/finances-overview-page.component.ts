import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { ProgressSpinner } from 'primeng/progressspinner';

import { OverviewDashboardDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/dashboard';
import { ErrorMessageService } from '../../../services/error-message.service';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import { OverviewDashboardCardsComponent } from '../components/overview-dashboard-cards/overview-dashboard-cards.component';
import { OverviewDashboardChartsComponent } from '../components/overview-dashboard-charts/overview-dashboard-charts.component';
import {
  AdvancedDatePickerComponent,
  DateRange,
} from '../components/wallet-entry-table/components/advanced-date-picker/advanced-date-picker.component';
import {
  MONTH_QUERY_PARAM_FORMAT,
  createMonthDateRange,
  readDateRangeFromQueryParams,
  syncDateQueryParams,
} from '../services/date-query-params.util';
import { OverviewDashboardService } from '../services/overview-dashboard.service';

@Component({
  selector: 'app-finances-overview-page',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    ReactiveFormsModule,
    AdvancedDatePickerComponent,
    ProgressSpinner,
    OverviewDashboardChartsComponent,
    OverviewDashboardCardsComponent,
  ],
  templateUrl: './finances-overview-page.component.html',
})
@UntilDestroy()
export class FinancesOverviewPageComponent {
  readonly dateControl = new FormControl<DateRange | undefined>(undefined);

  loading = false;
  dashboard: OverviewDashboardDto | undefined = undefined;
  dateRange: DateRange | undefined = undefined;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private overviewDashboardService: OverviewDashboardService,
    private errorMessageService: ErrorMessageService,
    private messageService: MessageService,
  ) {
    const initialDateRange =
      readDateRangeFromQueryParams(this.route.snapshot.queryParamMap, 'single') ?? createMonthDateRange(dayjs(), 'single');
    this.dateControl.setValue(initialDateRange, { emitEvent: false });
    this.applyDateRange(initialDateRange, false);

    this.dateControl.valueChanges.pipe(untilDestroyed(this)).subscribe(date => this.applyDateRange(date ?? undefined, true));
  }

  private applyDateRange(dateRange: DateRange | undefined, syncUrl: boolean) {
    this.dateRange = dateRange;

    if (syncUrl) {
      void syncDateQueryParams(this.route, this.router, dateRange, 'single');
    }

    void this.loadOverview();
  }

  private async loadOverview() {
    if (this.dateRange == null) {
      this.dashboard = undefined;
      return;
    }

    const month = this.dateRange.startDate.format(MONTH_QUERY_PARAM_FORMAT);

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

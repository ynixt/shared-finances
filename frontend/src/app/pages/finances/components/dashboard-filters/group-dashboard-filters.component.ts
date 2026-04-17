import { Component, input, output } from '@angular/core';

import { DashboardFeedFilters } from './dashboard-feed-filters.model';
import { DashboardFilterOption } from './dashboard-feed-filters.model';
import { DashboardFiltersBaseComponent } from './dashboard-filters-base.component';

@Component({
  selector: 'app-group-dashboard-filters',
  imports: [DashboardFiltersBaseComponent],
  templateUrl: './group-dashboard-filters.component.html',
})
export class GroupDashboardFiltersComponent {
  readonly filtersChange = output<DashboardFeedFilters>();

  readonly memberOptions = input<DashboardFilterOption[]>([]);
  readonly bankAccountOptions = input<DashboardFilterOption[]>([]);
  readonly creditCardOptions = input<DashboardFilterOption[]>([]);

  onFiltersChange(filters: DashboardFeedFilters) {
    this.filtersChange.emit(filters);
  }
}

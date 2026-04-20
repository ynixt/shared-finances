import { Component, output } from '@angular/core';

import { BankAccountService } from '../../services/bank-account.service';
import { CreditCardService } from '../../services/credit-card.service';
import { GroupService } from '../../services/group.service';
import { UserCategoriesService } from '../../services/user-categories.service';
import { DashboardFeedFilters } from './dashboard-feed-filters.model';
import { DashboardFilterOption } from './dashboard-feed-filters.model';
import { DashboardFiltersBaseComponent } from './dashboard-filters-base.component';

@Component({
  selector: 'app-individual-dashboard-filters',
  imports: [DashboardFiltersBaseComponent],
  templateUrl: './individual-dashboard-filters.component.html',
})
export class IndividualDashboardFiltersComponent {
  readonly filtersChange = output<DashboardFeedFilters>();
  private readonly dashboardFilterPageSize = 10;

  readonly groupOptionsGetter = this.loadGroups.bind(this);
  readonly bankAccountOptionsGetter = this.loadBankAccounts.bind(this);
  readonly creditCardOptionsGetter = this.loadCreditCards.bind(this);
  readonly categoryOptionsGetter = this.loadCategories.bind(this);

  constructor(
    private readonly groupService: GroupService,
    private readonly bankAccountService: BankAccountService,
    private readonly creditCardService: CreditCardService,
    private readonly userCategoriesService: UserCategoriesService,
  ) {}

  onFiltersChange(filters: DashboardFeedFilters) {
    this.filtersChange.emit(filters);
  }

  private async loadGroups(page = 0, query?: string | undefined): Promise<DashboardFilterOption[]> {
    const groups = await this.groupService.searchGroups(
      {
        page,
        size: this.dashboardFilterPageSize,
        sort: 'name',
      },
      query,
    );

    return groups.content.map(group => ({ id: group.id, label: group.name }));
  }

  private async loadBankAccounts(page = 0, query?: string | undefined): Promise<DashboardFilterOption[]> {
    const bankAccounts = await this.bankAccountService.getAllBankAccount(
      {
        page,
        size: this.dashboardFilterPageSize,
        sort: 'name',
      },
      query,
    );

    return bankAccounts.content.map(item => ({ id: item.id, label: item.name }));
  }

  private async loadCreditCards(page = 0, query?: string | undefined): Promise<DashboardFilterOption[]> {
    const creditCards = await this.creditCardService.getAllCreditCards(
      {
        page,
        size: this.dashboardFilterPageSize,
        sort: 'name',
      },
      query,
    );

    return creditCards.content.map(item => ({ id: item.id, label: item.name }));
  }

  private async loadCategories(page = 0, query?: string | undefined): Promise<DashboardFilterOption[]> {
    const categories = await this.userCategoriesService.getAllCategories(
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
}

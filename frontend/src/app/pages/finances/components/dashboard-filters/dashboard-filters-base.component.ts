import { Component, DestroyRef, inject, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { PagedMultiSelectComponent } from '../../../../components/paged-multi-select/paged-multi-select.component';
import {
  WalletEntryType,
  WalletEntryType__Obj,
} from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums/wallet-entry-type';
import { DashboardFeedFilters, DashboardFilterOption, EMPTY_DASHBOARD_FEED_FILTERS } from './dashboard-feed-filters.model';

type DashboardFiltersForm = {
  groupIds: FormControl<string[]>;
  memberIds: FormControl<string[]>;
  creditCardIds: FormControl<string[]>;
  bankAccountIds: FormControl<string[]>;
  entryTypes: FormControl<WalletEntryType[]>;
};

@Component({
  selector: 'app-dashboard-filters-base',
  imports: [ReactiveFormsModule, TranslatePipe, PagedMultiSelectComponent],
  templateUrl: './dashboard-filters-base.component.html',
})
export class DashboardFiltersBaseComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly translateService = inject(TranslateService);

  readonly contextLabel = input<string>('financesPage.overviewPage.filters.groups');
  readonly contextControl = input<'groupIds' | 'memberIds'>('groupIds');
  readonly showContext = input<boolean>(true);
  readonly contextOptionsGetterInput = input<(page: number, query?: string | undefined) => Promise<DashboardFilterOption[]>>();
  readonly bankAccountOptionsGetterInput = input<(page: number, query?: string | undefined) => Promise<DashboardFilterOption[]>>();
  readonly creditCardOptionsGetterInput = input<(page: number, query?: string | undefined) => Promise<DashboardFilterOption[]>>();
  readonly contextOptions = input<DashboardFilterOption[]>([]);
  readonly bankAccountOptions = input<DashboardFilterOption[]>([]);
  readonly creditCardOptions = input<DashboardFilterOption[]>([]);
  readonly multiSelectPageSize = 10;

  readonly filtersChange = output<DashboardFeedFilters>();

  readonly form = new FormGroup<DashboardFiltersForm>({
    groupIds: new FormControl<string[]>([], { nonNullable: true }),
    memberIds: new FormControl<string[]>([], { nonNullable: true }),
    creditCardIds: new FormControl<string[]>([], { nonNullable: true }),
    bankAccountIds: new FormControl<string[]>([], { nonNullable: true }),
    entryTypes: new FormControl<WalletEntryType[]>([], { nonNullable: true }),
  });

  readonly entryTypeOptions: Array<{ id: WalletEntryType; labelKey: string }> = [
    { id: WalletEntryType__Obj.REVENUE, labelKey: 'enums.walletEntryType.REVENUE' },
    { id: WalletEntryType__Obj.EXPENSE, labelKey: 'enums.walletEntryType.EXPENSE' },
    { id: WalletEntryType__Obj.TRANSFER, labelKey: 'enums.walletEntryType.TRANSFER' },
  ];
  readonly contextOptionsGetter = async (page: number, query?: string | undefined): Promise<DashboardFilterOption[]> => {
    const getter = this.contextOptionsGetterInput();
    if (getter != null) {
      return getter(page, query);
    }

    return this.paginateOptions(this.contextOptions(), page);
  };
  readonly bankAccountOptionsGetter = async (page: number, query?: string | undefined): Promise<DashboardFilterOption[]> => {
    const getter = this.bankAccountOptionsGetterInput();
    if (getter != null) {
      return getter(page, query);
    }

    return this.paginateOptions(this.bankAccountOptions(), page);
  };
  readonly creditCardOptionsGetter = async (page: number, query?: string | undefined): Promise<DashboardFilterOption[]> => {
    const getter = this.creditCardOptionsGetterInput();
    if (getter != null) {
      return getter(page, query);
    }

    return this.paginateOptions(this.creditCardOptions(), page);
  };
  readonly entryTypeOptionsGetter = async (page: number): Promise<Array<{ id: WalletEntryType; label: string }>> => {
    const translatedOptions = this.entryTypeOptions.map(option => ({
      id: option.id,
      label: this.translateService.instant(option.labelKey),
    }));
    return this.paginateOptions(translatedOptions, page);
  };

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.emitFilters());
    this.emitFilters();
  }

  clearFilters() {
    this.form.setValue(
      {
        groupIds: [],
        memberIds: [],
        creditCardIds: [],
        bankAccountIds: [],
        entryTypes: [],
      },
      { emitEvent: false },
    );
    this.emitFilters();
  }

  serializeFilters(): DashboardFeedFilters {
    const value = this.form.getRawValue();
    return {
      ...EMPTY_DASHBOARD_FEED_FILTERS,
      groupIds: value.groupIds,
      memberIds: value.memberIds,
      creditCardIds: value.creditCardIds,
      bankAccountIds: value.bankAccountIds,
      entryTypes: value.entryTypes,
    };
  }

  private emitFilters() {
    this.filtersChange.emit(this.serializeFilters());
  }

  private paginateOptions<TOption>(options: TOption[], page: number): TOption[] {
    const start = page * this.multiSelectPageSize;
    return options.slice(start, start + this.multiSelectPageSize);
  }
}

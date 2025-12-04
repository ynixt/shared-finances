import { NgClass, formatDate } from '@angular/common';
import { Component, computed, effect, inject, input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faDollarSign } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonDirective } from 'primeng/button';
import { DataView } from 'primeng/dataview';
import { Skeleton } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { InfinitePaginatorComponent } from '../../../../components/infinite-paginator/infinite-paginator.component';
import { CursorPage } from '../../../../models/cursor-pagination';
import { EntryForListDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { LocalCurrencyPipe } from '../../../../pipes/local-currency.pipe';
import { LocalDatePipe, LocalDatePipeService } from '../../../../pipes/local-date.pipe';
import { WalletEntryService } from '../../services/wallet-entry.service';
import { DateRange } from './components/advanced-date-picker/advanced-date-picker.component';
import { EntryDescriptionComponent } from './components/entry-description/entry-description.component';
import { EntryStatusComponent } from './components/entry-status/entry-status.component';
import { EntryTypeComponent } from './components/entry-type/entry-type.component';

@Component({
  selector: 'app-wallet-entry-table',
  imports: [
    TableModule,
    DataView,
    NgClass,
    EntryStatusComponent,
    LocalDatePipe,
    EntryDescriptionComponent,
    LocalCurrencyPipe,
    EntryTypeComponent,
    Skeleton,
    InfinitePaginatorComponent,
    FormsModule,
    TranslatePipe,
    ButtonDirective,
    FaIconComponent,
    RouterLink,
  ],
  templateUrl: './wallet-entry-table.component.html',
  styleUrl: './wallet-entry-table.component.scss',
})
export class WalletEntryTableComponent {
  readonly pageSize = 30;
  readonly walletEntryService = inject(WalletEntryService);
  readonly localDatePipeService = inject(LocalDatePipeService);
  readonly pages: Map<number, CursorPage<EntryForListDto>> = new Map();
  readonly skeletons = Array.from({ length: this.pageSize > 10 ? 10 : this.pageSize }, (_, i) => i + 1);
  readonly dateRange = input<DateRange | undefined>(undefined);
  readonly dateRangeText = computed(() => {
    const dateRange = this.dateRange();

    if (dateRange == null) return '';
    if (dateRange.sameMonth) return this.localDatePipeService.transform(dateRange.startDate, 'MMMM, yyyy');

    return `${this.localDatePipeService.transform(dateRange.startDate, 'short')} - ${this.localDatePipeService.transform(dateRange.endDate, 'short')}`;
  });

  walletItemIds = input<string[] | undefined>();

  currentPageNumber = 0;
  page: CursorPage<EntryForListDto> | undefined;

  constructor() {
    effect(async () => {
      const walletItemIds = this.walletItemIds();
      const dateRange = this.dateRange();

      this.currentPageNumber = 0;
      this.page = undefined;
      this.pages.clear();

      const page = await this.entryFetcher(walletItemIds, dateRange);

      this.page = page;
      this.pages.set(0, page);
    });
  }

  entryFetcher = async (
    walletItemIds: string[] | undefined,
    dateRange: DateRange | undefined,
    nextCursor?:
      | {
          [p: string]: any;
        }
      | undefined,
  ): Promise<CursorPage<EntryForListDto>> => {
    return await this.walletEntryService.listWalletEntries(
      {
        size: this.pageSize,
        nextCursor: nextCursor,
      },
      {
        walletItemIds,
        minimumDate: dateRange?.startDate?.format('YYYY-MM-DD'),
        maximumDate: dateRange?.endDate?.format('YYYY-MM-DD'),
      },
    );
  };

  async onPageChange(newPage: number) {
    if (this.pages.has(newPage)) {
      this.page = this.pages.get(newPage);
      this.currentPageNumber = newPage;
    } else {
      const nextCursor = this.page?.nextCursor;
      this.page = undefined;

      const page = await this.entryFetcher(this.walletItemIds(), this.dateRange(), nextCursor);
      this.page = page;
      this.pages.set(++this.currentPageNumber, page);
    }
  }

  protected readonly newTransactionButtonIcon = faDollarSign;
}

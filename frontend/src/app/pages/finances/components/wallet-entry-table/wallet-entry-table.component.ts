import { NgClass } from '@angular/common';
import { Component, computed, effect, inject, input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faDollarSign } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import dayjs from 'dayjs';
import { ButtonDirective } from 'primeng/button';
import { DataView } from 'primeng/dataview';
import { Skeleton } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { InfinitePaginatorComponent } from '../../../../components/infinite-paginator/infinite-paginator.component';
import { CursorPage } from '../../../../models/cursor-pagination';
import { EntryForListDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { LocalCurrencyPipe } from '../../../../pipes/local-currency.pipe';
import { LocalDatePipe, LocalDatePipeService } from '../../../../pipes/local-date.pipe';
import { ONLY_DATE_FORMAT } from '../../../../util/date-util';
import { UserActionEventService } from '../../services/user-action-event.service';
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
@UntilDestroy()
export class WalletEntryTableComponent {
  readonly walletEntryService = inject(WalletEntryService);
  readonly localDatePipeService = inject(LocalDatePipeService);
  readonly userActionEventService = inject(UserActionEventService);

  readonly pageSize = 30;
  readonly pages: Map<number, CursorPage<EntryForListDto>> = new Map();
  readonly skeletons = Array.from({ length: this.pageSize > 10 ? 10 : this.pageSize }, (_, i) => i + 1);
  currentPageNumber = 0;
  page: CursorPage<EntryForListDto> | undefined;

  readonly loading = input<boolean>(false);
  readonly dateRange = input<DateRange | undefined>(undefined);
  readonly creditCardBillId = input<string | undefined | null>(undefined);
  readonly creditCardBillDate = input<dayjs.Dayjs | undefined>(undefined);
  readonly noWalletEntryFoundMessage = input<string | undefined>(undefined);
  readonly noWalletEntryFoundDynamicMessage = computed(() => {
    if (this.noWalletEntryFoundMessage() == null) {
      return this.dateRangeText();
    }

    return this.noWalletEntryFoundMessage();
  });

  walletItemId = input<string | undefined>();

  private readonly dateRangeText = computed(() => {
    const dateRange = this.dateRange();

    if (dateRange == null) return '';
    if (dateRange.sameMonth) return this.localDatePipeService.transform(dateRange.startDate, 'MMMM, yyyy');

    return `${this.localDatePipeService.transform(dateRange.startDate, 'short')} - ${this.localDatePipeService.transform(dateRange.endDate, 'short')}`;
  });

  constructor() {
    effect(async () => {
      const wId = this.walletItemId();
      const dRange = this.dateRange();
      const isLoading = this.loading();

      if (isLoading) return;

      await this.loadInitialData();
    });

    this.userActionEventService.transactionInserted$.pipe(untilDestroyed(this)).subscribe(dto => this.newTransactionInserted(dto));
  }

  entryFetcher = async (
    walletItemId: string | undefined,
    dateRange: DateRange | undefined,
    nextCursor?:
      | {
          [p: string]: any;
        }
      | undefined,
  ): Promise<CursorPage<EntryForListDto>> => {
    const billId = this.creditCardBillId();
    const billDate = this.creditCardBillDate();

    return await this.walletEntryService.listWalletEntries(
      {
        size: this.pageSize,
        nextCursor: nextCursor,
      },
      {
        walletItemId,
        billId,
        billDate: billDate?.format(ONLY_DATE_FORMAT),
        minimumDate: dateRange?.startDate?.format(ONLY_DATE_FORMAT),
        maximumDate: dateRange?.endDate?.format(ONLY_DATE_FORMAT),
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

      const page = await this.entryFetcher(this.walletItemId(), this.dateRange(), nextCursor);
      this.page = page;
      this.pages.set(++this.currentPageNumber, page);
    }
  }

  private async loadInitialData() {
    this.currentPageNumber = 0;
    this.page = undefined;
    this.pages.clear();

    const page = await this.entryFetcher(this.walletItemId(), this.dateRange());

    this.page = page;
    this.pages.set(0, page);
  }

  private async reload() {
    await this.loadInitialData();
  }

  private newTransactionInserted(dto: EntryForListDto) {
    // TODO: improve this

    const dateRange = this.dateRange();
    const walletItemId = this.walletItemId();
    const creditCardBillDate = this.creditCardBillDate();

    if (dateRange == null || walletItemId == null || (dto.origin.id != walletItemId && dto.target?.id != walletItemId)) return;

    if (
      (creditCardBillDate != null &&
        ((dto.originBillDate && creditCardBillDate.isSame(dayjs(dto.originBillDate))) ||
          (dto.targetBillDate && creditCardBillDate.isSame(dayjs(dto.targetBillDate))))) ||
      ((dateRange.startDate.isBefore(dto.date) || dateRange.startDate.isSame(dto.date)) &&
        (dateRange.endDate == null || dateRange.endDate.isAfter(dto.date) || dateRange.endDate.isSame(dto.date)))
    ) {
      this.reload();
    }
  }

  protected readonly newTransactionButtonIcon = faDollarSign;
}

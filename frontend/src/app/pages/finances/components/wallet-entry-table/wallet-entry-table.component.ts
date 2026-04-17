import { NgClass } from '@angular/common';
import { Component, computed, effect, inject, input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faDollarSign, faPenToSquare, faTrashCan } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import dayjs from 'dayjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { DataView } from 'primeng/dataview';
import { Skeleton } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { InfinitePaginatorComponent } from '../../../../components/infinite-paginator/infinite-paginator.component';
import { CursorPage } from '../../../../models/cursor-pagination';
import { EventForListDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { ScheduledEditScope, WalletEntryType } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
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
    ConfirmDialog,
    FaIconComponent,
    RouterLink,
  ],
  templateUrl: './wallet-entry-table.component.html',
  styleUrl: './wallet-entry-table.component.scss',
  providers: [ConfirmationService],
})
@UntilDestroy()
export class WalletEntryTableComponent {
  readonly walletEntryService = inject(WalletEntryService);
  readonly localDatePipeService = inject(LocalDatePipeService);
  readonly userActionEventService = inject(UserActionEventService);
  readonly confirmationService = inject(ConfirmationService);
  private readonly translateService = inject(TranslateService);

  readonly pageSize = 30;
  readonly pages: Map<number, CursorPage<EventForListDto>> = new Map();
  readonly skeletons = Array.from({ length: this.pageSize > 10 ? 10 : this.pageSize }, (_, i) => i + 1);
  currentPageNumber = 0;
  page: CursorPage<EventForListDto> | undefined;

  readonly title = input<string | undefined>('financesPage.overviewPage.feed.title');
  readonly loading = input<boolean>(false);
  readonly dateRange = input<DateRange | undefined>(undefined);
  readonly groupIds = input<string[] | undefined>(undefined);
  readonly creditCardIds = input<string[] | undefined>(undefined);
  readonly bankAccountIds = input<string[] | undefined>(undefined);
  readonly entryTypes = input<WalletEntryType[] | undefined>(undefined);
  readonly refreshKey = input<number>(0);
  readonly listenToUserEvents = input<boolean>(true);
  readonly creditCardBillId = input<string | undefined | null>(undefined);
  readonly creditCardBillDate = input<dayjs.Dayjs | undefined>(undefined);
  readonly noWalletEntryFoundMessage = input<string | undefined>(undefined);
  scopeSelectionEntry: EventForListDto | null = null;
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
      const filterGroupIds = this.groupIds();
      const filterCreditCardIds = this.creditCardIds();
      const filterBankAccountIds = this.bankAccountIds();
      const filterEntryTypes = this.entryTypes();
      const refreshKey = this.refreshKey();
      const isLoading = this.loading();
      void wId;
      void dRange;
      void filterGroupIds;
      void filterCreditCardIds;
      void filterBankAccountIds;
      void filterEntryTypes;
      void refreshKey;

      if (isLoading) return;

      await this.loadInitialData();
    });

    this.userActionEventService.transactionInserted$.pipe(untilDestroyed(this)).subscribe(dto => this.newTransactionInserted(dto));
    this.userActionEventService.transactionUpdated$.pipe(untilDestroyed(this)).subscribe(dto => this.newTransactionInserted(dto));
    this.userActionEventService.transactionDeleted$.pipe(untilDestroyed(this)).subscribe(dto => this.newTransactionInserted(dto));
    this.userActionEventService.resyncRequired$.pipe(untilDestroyed(this)).subscribe(() => void this.reload());
  }

  entryFetcher = async (
    walletItemId: string | undefined,
    dateRange: DateRange | undefined,
    nextCursor?:
      | {
          [p: string]: any;
        }
      | undefined,
  ): Promise<CursorPage<EventForListDto>> => {
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
        groupIds: this.groupIds(),
        creditCardIds: this.creditCardIds(),
        bankAccountIds: this.bankAccountIds(),
        entryTypes: this.entryTypes(),
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

  private newTransactionInserted(dto: EventForListDto) {
    if (!this.listenToUserEvents()) return;
    if (!this.shouldReloadForEvent(dto)) return;
    void this.reload();
  }

  private shouldReloadForEvent(dto: EventForListDto): boolean {
    const dateRange = this.dateRange();
    if (dateRange == null) {
      return false;
    }

    const eventDate = dayjs(dto.date);
    const isDateInRange =
      (dateRange.startDate.isBefore(eventDate) || dateRange.startDate.isSame(eventDate, 'day')) &&
      (dateRange.endDate == null || dateRange.endDate.isAfter(eventDate) || dateRange.endDate.isSame(eventDate, 'day'));
    if (!isDateInRange) {
      return false;
    }

    const walletItemId = this.walletItemId();
    if (walletItemId != null && dto.entries.find(entry => entry.walletItemId === walletItemId) == null) {
      return false;
    }

    const bankAccountIds = this.bankAccountIds() ?? [];
    if (bankAccountIds.length > 0 && dto.entries.find(entry => bankAccountIds.includes(entry.walletItemId)) == null) {
      return false;
    }

    const creditCardIds = this.creditCardIds() ?? [];
    if (creditCardIds.length > 0 && dto.entries.find(entry => creditCardIds.includes(entry.walletItemId)) == null) {
      return false;
    }

    const groupIds = this.groupIds() ?? [];
    if (groupIds.length > 0 && (dto.group?.id == null || !groupIds.includes(dto.group.id))) {
      return false;
    }

    const entryTypes = this.entryTypes() ?? [];
    if (entryTypes.length > 0 && !entryTypes.includes(dto.type)) {
      return false;
    }

    const creditCardBillDate = this.creditCardBillDate();
    if (creditCardBillDate != null) {
      return dto.entries.find(entry => entry.billDate && creditCardBillDate.isSame(dayjs(entry.billDate), 'day')) != null;
    }

    return true;
  }

  canEdit(entry: EventForListDto): boolean {
    return entry.id != null || entry.recurrenceConfigId != null;
  }

  canDelete(entry: EventForListDto): boolean {
    return entry.id != null || entry.recurrenceConfigId != null;
  }

  editRouterLink(entry: EventForListDto): string[] {
    if (entry.recurrenceConfigId != null && entry.id == null) {
      return ['/app/transactions/scheduler-manager/edit', entry.recurrenceConfigId];
    }

    return ['/app/transactions/edit', entry.id!!];
  }

  editQueryParams(entry: EventForListDto): { [key: string]: any } | undefined {
    if (entry.recurrenceConfigId != null && entry.id == null) {
      return { withFuture: true };
    }

    return undefined;
  }

  startDelete(entry: EventForListDto) {
    if (!this.canDelete(entry)) {
      return;
    }

    if (entry.recurrenceConfigId != null && this.shouldAskScope(entry)) {
      this.scopeSelectionEntry = entry;
      return;
    }

    if (entry.recurrenceConfigId != null) {
      void this.deleteScheduled(entry);
      return;
    }

    if (entry.id != null) {
      this.deleteOneOff(entry.id);
    }
  }

  cancelScopeSelection() {
    this.scopeSelectionEntry = null;
  }

  chooseOnlyThis() {
    const selectedEntry = this.scopeSelectionEntry;
    if (selectedEntry == null) {
      return;
    }

    void this.deleteScheduled(selectedEntry, 'ONLY_THIS');
  }

  chooseThisAndFuture() {
    const selectedEntry = this.scopeSelectionEntry;
    if (selectedEntry == null) {
      return;
    }

    void this.deleteScheduled(selectedEntry, 'THIS_AND_FUTURE');
  }

  chooseAllSeries() {
    const selectedEntry = this.scopeSelectionEntry;
    if (selectedEntry == null) {
      return;
    }

    void this.deleteScheduled(selectedEntry, 'ALL_SERIES');
  }

  private shouldAskScope(entry: EventForListDto): boolean {
    return entry.recurrenceConfig?.paymentType !== 'UNIQUE';
  }

  private deleteOneOff(id: string) {
    this.confirmationService.confirm({
      message: this.translateService.instant('financesPage.transactionsPage.deleteConfirm'),
      header: this.translateService.instant('general.confirmation'),
      closable: true,
      closeOnEscape: true,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.translateService.instant('general.delete'),
      rejectLabel: this.translateService.instant('general.cancel'),
      acceptButtonProps: {
        severity: 'danger',
      },
      rejectButtonProps: {
        severity: 'secondary',
      },
      accept: () => {
        void this.confirmDeleteOneOff(id);
      },
    });
  }

  private async confirmDeleteOneOff(id: string) {
    await this.walletEntryService.deleteWalletEntry(id);
    await this.reload();
  }

  private async deleteScheduled(entry: EventForListDto, scope?: ScheduledEditScope) {
    if (entry.recurrenceConfigId == null) {
      return;
    }

    await this.walletEntryService.deleteScheduledEntry(entry.recurrenceConfigId, {
      occurrenceDate: dayjs(entry.date).format(ONLY_DATE_FORMAT),
      scope: entry.recurrenceConfig?.paymentType === 'UNIQUE' ? undefined : scope,
    });

    this.cancelScopeSelection();
    await this.reload();
  }

  protected showDualTransferAmounts(entry: EventForListDto): boolean {
    return entry.type === 'TRANSFER' && entry.targetValue != null;
  }

  protected getTransferOriginEntry(entry: EventForListDto) {
    return entry.entries.find(item => item.value < 0) ?? entry.entries[0];
  }

  protected getTransferTargetEntry(entry: EventForListDto) {
    const origin = this.getTransferOriginEntry(entry);
    return entry.entries.find(item => item.walletItemId !== origin.walletItemId) ?? entry.entries[1] ?? entry.entries[0];
  }

  protected getTransferOriginDisplayValue(entry: EventForListDto): number {
    return entry.originValue ?? Math.abs(this.getTransferOriginEntry(entry).value ?? 0);
  }

  protected getTransferTargetDisplayValue(entry: EventForListDto): number | null {
    return entry.targetValue ?? null;
  }

  protected readonly newTransactionButtonIcon = faDollarSign;
  protected readonly editTransactionButtonIcon = faPenToSquare;
  protected readonly deleteTransactionButtonIcon = faTrashCan;
}

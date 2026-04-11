import { NgClass } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faPenToSquare, faTrashCan } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { DataView } from 'primeng/dataview';
import { SelectButton } from 'primeng/selectbutton';
import { Skeleton } from 'primeng/skeleton';

import {
  DeleteScheduledEntryDto,
  EventForListDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import {
  ScheduledEditScope,
  ScheduledEditScope__Obj,
  ScheduledExecutionFilter,
  ScheduledExecutionFilter__Obj,
} from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { LocalCurrencyPipe } from '../../../../pipes/local-currency.pipe';
import { LocalDatePipe } from '../../../../pipes/local-date.pipe';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { ONLY_DATE_FORMAT } from '../../../../util/date-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { EntryDescriptionComponent } from '../../components/wallet-entry-table/components/entry-description/entry-description.component';
import { EntryStatusComponent } from '../../components/wallet-entry-table/components/entry-status/entry-status.component';
import { EntryTypeComponent } from '../../components/wallet-entry-table/components/entry-type/entry-type.component';
import { UserActionEventService } from '../../services/user-action-event.service';
import { WalletEntryService } from '../../services/wallet-entry.service';

type FilterOption = {
  label: string;
  value: ScheduledExecutionFilter;
};

@Component({
  selector: 'app-scheduled-execution-manager-page',
  imports: [
    FinancesTitleBarComponent,
    NgClass,
    TranslatePipe,
    SelectButton,
    ButtonDirective,
    FaIconComponent,
    DataView,
    Skeleton,
    FormsModule,
    LocalDatePipe,
    LocalCurrencyPipe,
    EntryDescriptionComponent,
    EntryStatusComponent,
    EntryTypeComponent,
    RouterLink,
  ],
  templateUrl: './scheduled-execution-manager-page.component.html',
  styleUrl: './scheduled-execution-manager-page.component.scss',
})
@UntilDestroy()
export class ScheduledExecutionManagerPageComponent implements OnInit {
  readonly ScheduledExecutionFilter = ScheduledExecutionFilter__Obj;

  private readonly walletEntryService = inject(WalletEntryService);
  private readonly userActionEventService = inject(UserActionEventService);
  private readonly errorMessageService = inject(ErrorMessageService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);

  readonly loading = signal(false);
  readonly deleting = signal(false);
  selectedFilter: ScheduledExecutionFilter = ScheduledExecutionFilter__Obj.FUTURE;
  readonly entries = signal<EventForListDto[]>([]);
  readonly skeletons = Array.from({ length: 10 }, (_, i) => i + 1);
  readonly deletingEntry = signal<EventForListDto | null>(null);

  readonly filterOptions: FilterOption[] = [
    { label: 'financesPage.scheduleManager.filters.future', value: ScheduledExecutionFilter__Obj.FUTURE },
    { label: 'financesPage.scheduleManager.filters.all', value: ScheduledExecutionFilter__Obj.ALL },
    { label: 'financesPage.scheduleManager.filters.alreadyGenerated', value: ScheduledExecutionFilter__Obj.ALREADY_GENERATED },
  ];

  async ngOnInit() {
    await this.loadEntries();

    this.userActionEventService.transactionInserted$.pipe(untilDestroyed(this)).subscribe(dto => void this.refreshWhenScheduledChange(dto));
    this.userActionEventService.transactionUpdated$.pipe(untilDestroyed(this)).subscribe(dto => void this.refreshWhenScheduledChange(dto));
    this.userActionEventService.transactionDeleted$.pipe(untilDestroyed(this)).subscribe(dto => void this.refreshWhenScheduledChange(dto));
    this.userActionEventService.resyncRequired$.pipe(untilDestroyed(this)).subscribe(() => void this.loadEntries());
  }

  async onFilterChange() {
    this.deletingEntry.set(null);
    await this.loadEntries();
  }

  editRouterLink(entry: EventForListDto): string[] {
    return ['/app/transactions/scheduler-manager/edit', entry.recurrenceConfigId!!];
  }

  requestDelete(entry: EventForListDto) {
    if (entry.recurrenceConfigId == null) {
      return;
    }

    if (this.isUnique(entry)) {
      void this.deleteScheduled(entry);
      return;
    }

    this.deletingEntry.set(entry);
  }

  cancelDeleteScopeSelection() {
    this.deletingEntry.set(null);
  }

  chooseDeleteOnlyThis() {
    const entry = this.deletingEntry();
    if (entry == null) {
      return;
    }

    void this.deleteScheduled(entry, ScheduledEditScope__Obj.ONLY_THIS);
  }

  chooseDeleteThisAndFuture() {
    const entry = this.deletingEntry();
    if (entry == null) {
      return;
    }

    void this.deleteScheduled(entry, ScheduledEditScope__Obj.THIS_AND_FUTURE);
  }

  chooseDeleteAllSeries() {
    const entry = this.deletingEntry();
    if (entry == null) {
      return;
    }

    void this.deleteScheduled(entry, ScheduledEditScope__Obj.ALL_SERIES);
  }

  private async loadEntries() {
    this.loading.set(true);

    try {
      this.entries.set(
        await this.walletEntryService.listScheduledExecutions({
          filter: this.selectedFilter,
        }),
      );
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
      throw error;
    } finally {
      this.loading.set(false);
    }
  }

  private async refreshWhenScheduledChange(dto: EventForListDto) {
    if (dto.recurrenceConfigId == null) {
      return;
    }

    await this.loadEntries();
  }

  private async deleteScheduled(entry: EventForListDto, scope?: ScheduledEditScope) {
    if (entry.recurrenceConfigId == null || this.deleting()) {
      return;
    }

    if (!window.confirm(this.translateService.instant('financesPage.transactionsPage.deleteConfirm'))) {
      return;
    }

    this.deleting.set(true);

    try {
      const payload: DeleteScheduledEntryDto = {
        occurrenceDate: dayjs(entry.date).format(ONLY_DATE_FORMAT),
        scope: this.isUnique(entry) ? undefined : scope,
      };

      await this.walletEntryService.deleteScheduledEntry(entry.recurrenceConfigId, payload);
      this.deletingEntry.set(null);
      await this.loadEntries();
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
      throw error;
    } finally {
      this.deleting.set(false);
    }
  }

  protected showDualTransferAmounts(entry: EventForListDto): boolean {
    return entry.type === 'TRANSFER' && entry.targetValue != null;
  }

  protected getOriginEntry(entry: EventForListDto) {
    return entry.entries.find(item => item.value < 0) ?? entry.entries[0];
  }

  protected getTargetEntry(entry: EventForListDto) {
    const origin = this.getOriginEntry(entry);
    return entry.entries.find(item => item.walletItemId !== origin.walletItemId) ?? entry.entries[1] ?? entry.entries[0];
  }

  protected getOriginDisplayValue(entry: EventForListDto): number {
    if (entry.type === 'TRANSFER') {
      return entry.originValue ?? Math.abs(this.getOriginEntry(entry).value ?? 0);
    }

    return Math.abs(entry.entries[0].value);
  }

  protected getTargetDisplayValue(entry: EventForListDto): number | null {
    if (entry.type !== 'TRANSFER') {
      return null;
    }

    return entry.targetValue ?? null;
  }

  protected statusLabel(entry: EventForListDto): string {
    if (entry.id == null) {
      return 'financesPage.scheduleManager.status.future';
    }

    return 'financesPage.scheduleManager.status.generated';
  }

  protected recurrenceLabel(entry: EventForListDto): string {
    const recurrence = entry.recurrenceConfig;
    if (recurrence == null) {
      return '-';
    }

    const paymentLabel = this.translateService.instant(`enums.paymentType.${recurrence.paymentType}`);
    if (recurrence.paymentType === 'UNIQUE') {
      return paymentLabel;
    }

    const periodicityLabel = this.translateService.instant(`enums.recurrenceType.${recurrence.periodicity}`);
    return `${paymentLabel} / ${periodicityLabel}`;
  }

  protected rowTrack(entry: EventForListDto): string {
    return `${entry.recurrenceConfigId ?? 'no-rec'}-${entry.date}-${entry.id ?? 'future'}`;
  }

  private isUnique(entry: EventForListDto): boolean {
    return entry.recurrenceConfig?.paymentType === 'UNIQUE';
  }

  protected readonly editTransactionButtonIcon = faPenToSquare;
  protected readonly deleteTransactionButtonIcon = faTrashCan;
}

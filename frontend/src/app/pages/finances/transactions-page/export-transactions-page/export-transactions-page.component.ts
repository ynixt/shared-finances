import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';

import {
  CreateExportDto,
  ExportBatchDto,
  ExportBatchStatusEventDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/exports';
import { WalletItemSearchResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { WalletEntryType } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { DateRange } from '../../components/wallet-entry-table/components/advanced-date-picker/advanced-date-picker.component';
import { ExportService } from '../../services/export.service';
import { GroupCategoriesService } from '../../services/group-categories.service';
import { GroupWalletItemService } from '../../services/group-wallet-item.service';
import { GroupService } from '../../services/group.service';
import { UserActionEventService } from '../../services/user-action-event.service';
import { UserCategoriesService } from '../../services/user-categories.service';
import { WalletItemService } from '../../services/wallet-item.service';
import { retainAssociatedExportSelections } from './export-filter-cascade';
import { ExportFiltersComponent } from './export-filters.component';
import { ExportHistoryComponent } from './export-history.component';

@Component({
  selector: 'app-export-transactions-page',
  imports: [ReactiveFormsModule, TranslatePipe, FinancesTitleBarComponent, ExportFiltersComponent, ExportHistoryComponent],
  templateUrl: './export-transactions-page.component.html',
})
export class ExportTransactionsPageComponent implements OnInit {
  private static readonly FILTER_PAGE_SIZE = 10;

  private readonly formBuilder = inject(FormBuilder);
  private readonly groupService = inject(GroupService);
  private readonly walletItemService = inject(WalletItemService);
  private readonly groupWalletItemService = inject(GroupWalletItemService);
  private readonly userCategoriesService = inject(UserCategoriesService);
  private readonly groupCategoriesService = inject(GroupCategoriesService);
  private readonly exportService = inject(ExportService);
  private readonly events = inject(UserActionEventService);
  private readonly messages = inject(MessageService);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly downloadsInProgress = new Set<string>();
  private readonly removedBatchIds = new Set<string>();

  readonly form = this.formBuilder.group({
    groupId: this.formBuilder.control<string | null>(null),
    dateRange: this.formBuilder.control<DateRange>({ startDate: dayjs().startOf('month'), endDate: dayjs().endOf('month') }),
    walletItemIds: this.formBuilder.control<string[]>([]),
    categoryIds: this.formBuilder.control<string[]>([]),
    entryTypes: this.formBuilder.control<WalletEntryType[]>([]),
    tags: this.formBuilder.control<string[]>([]),
    confirmed: this.formBuilder.control<boolean | null>(null),
    billDateMode: this.formBuilder.control(false, { nonNullable: true }),
    format: this.formBuilder.control<'CSV' | 'XLSX'>('CSV', { nonNullable: true }),
  });

  batches: ExportBatchDto[] = [];
  groups = [] as Awaited<ReturnType<GroupService['getAllGroups']>>;
  selectedGroupId: string | null = null;
  loading = true;
  submitting = false;

  readonly walletItemsGetter = this.loadWalletItems.bind(this);
  readonly categoriesGetter = this.loadCategories.bind(this);

  get inProgress(): ExportBatchDto | undefined {
    return this.batches.find(batch => batch.status === 'QUEUED' || batch.status === 'RUNNING');
  }

  async ngOnInit(): Promise<void> {
    this.events.exportBatchAction$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(event => {
      if (event.type === 'DELETE') {
        const id = event.data as string;
        this.removedBatchIds.add(id);
        this.batches = this.batches.filter(batch => batch.id !== id);
      } else {
        this.applyBatchEvent(event.data as ExportBatchStatusEventDto);
      }
    });
    try {
      const [groups, history] = await Promise.all([this.groupService.getAllGroups(), this.exportService.list()]);
      this.groups = groups;
      history
        .filter(batch => !this.removedBatchIds.has(batch.id) && !this.batches.some(current => current.id === batch.id))
        .forEach(batch => this.upsert(batch));
    } finally {
      this.loading = false;
    }
  }

  async groupChanged(groupId: string | null): Promise<void> {
    this.selectedGroupId = groupId;
    const selectedWalletItemIds = this.form.value.walletItemIds ?? [];
    const selectedCategoryIds = this.form.value.categoryIds ?? [];
    if (selectedWalletItemIds.length === 0 && selectedCategoryIds.length === 0) return;

    const [walletItems, categories] = await Promise.all([this.loadAllWalletItems(), this.loadAllCategories()]);
    if (this.selectedGroupId !== groupId) return;
    this.form.patchValue(
      retainAssociatedExportSelections(
        {
          walletItemIds: selectedWalletItemIds,
          categoryIds: selectedCategoryIds,
        },
        walletItems,
        categories,
      ),
    );
  }

  private async loadWalletItems(page = 0, query?: string): Promise<WalletItemSearchResponseDto[]> {
    const request = { page, size: ExportTransactionsPageComponent.FILTER_PAGE_SIZE, sort: 'name' };
    const result =
      this.selectedGroupId == null
        ? await this.walletItemService.getAllItems(request, false, query)
        : await this.groupWalletItemService.getAllItems(this.selectedGroupId, request, false, query);
    return result.content;
  }

  private async loadCategories(page = 0, query?: string): Promise<CategoryDto[]> {
    const params = { onlyRoot: false, mountChildren: false, query };
    const request = { page, size: ExportTransactionsPageComponent.FILTER_PAGE_SIZE, sort: 'name' };
    const result =
      this.selectedGroupId == null
        ? await this.userCategoriesService.getAllCategories(params, request)
        : await this.groupCategoriesService.getAllCategories(this.selectedGroupId, params, request);
    return result.content;
  }

  private loadAllWalletItems(): Promise<WalletItemSearchResponseDto[]> {
    return this.loadEveryPage(page => this.loadWalletItems(page));
  }

  private loadAllCategories(): Promise<CategoryDto[]> {
    return this.loadEveryPage(page => this.loadCategories(page));
  }

  private async loadEveryPage<T>(loader: (page: number) => Promise<T[]>): Promise<T[]> {
    const items: T[] = [];
    for (let page = 0; ; page += 1) {
      const content = await loader(page);
      items.push(...content);
      if (content.length < ExportTransactionsPageComponent.FILTER_PAGE_SIZE) return items;
    }
  }

  async submit(): Promise<void> {
    if (this.inProgress != null) return;
    const value = this.form.getRawValue();
    const dateRange = value.dateRange;
    const request: CreateExportDto = {
      format: value.format,
      filter: {
        groupId: value.groupId,
        dateFrom: dateRange?.startDate.format('YYYY-MM-DD'),
        dateTo: dateRange?.endDate?.format('YYYY-MM-DD'),
        walletItemIds: value.walletItemIds ?? [],
        categoryIds: value.categoryIds ?? [],
        entryTypes: value.entryTypes ?? [],
        tags: value.tags ?? [],
        confirmed: value.confirmed,
        billDateMode: value.billDateMode,
      },
    };
    this.submitting = true;
    try {
      const batch = await this.exportService.create(request);
      this.upsert(batch);
      this.messages.add({ severity: 'info', summary: this.text('notifications.started') });
    } finally {
      this.submitting = false;
    }
  }

  async download(batch: ExportBatchDto): Promise<void> {
    if (this.downloadsInProgress.has(batch.id)) return;
    this.downloadsInProgress.add(batch.id);
    try {
      const metadata = await this.exportService.download(batch);
      const current = this.batches.find(existing => existing.id === batch.id);
      if (current != null) {
        this.upsert({
          ...current,
          firstDownloadedAt: metadata.firstDownloadedAt ?? current.firstDownloadedAt,
          downloadExpiresAt: metadata.downloadExpiresAt ?? current.downloadExpiresAt,
        });
      }
    } finally {
      this.downloadsInProgress.delete(batch.id);
    }
  }

  async delete(batch: ExportBatchDto): Promise<void> {
    await this.exportService.delete(batch.id);
    this.removedBatchIds.add(batch.id);
    this.batches = this.batches.filter(existing => existing.id !== batch.id);
  }

  private applyBatchEvent(batch: ExportBatchStatusEventDto): void {
    this.upsert(batch);
  }

  private upsert(batch: ExportBatchDto): void {
    this.batches = [batch, ...this.batches.filter(existing => existing.id !== batch.id)].sort((a, b) =>
      String(b.createdAt).localeCompare(String(a.createdAt)),
    );
  }

  private text(key: string): string {
    return this.translate.instant(`financesPage.transactionsPage.exportPage.${key}`);
  }
}

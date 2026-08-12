import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';

import { Subject } from 'rxjs';

import { MessageService } from 'primeng/api';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ExportBatchDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/exports';
import { ExportService } from '../../services/export.service';
import { GroupCategoriesService } from '../../services/group-categories.service';
import { GroupWalletItemService } from '../../services/group-wallet-item.service';
import { GroupService } from '../../services/group.service';
import { UserActionEventService } from '../../services/user-action-event.service';
import { UserCategoriesService } from '../../services/user-categories.service';
import { WalletItemService } from '../../services/wallet-item.service';
import { ExportTransactionsPageComponent } from './export-transactions-page.component';

describe('export filter pagination', () => {
  const walletItems = { getAllItems: vi.fn() };
  const groupWalletItems = { getAllItems: vi.fn() };
  const categories = { getAllCategories: vi.fn() };
  const groupCategories = { getAllCategories: vi.fn() };
  const groups = { getAllGroups: vi.fn() };
  const exportService = { create: vi.fn(), delete: vi.fn(), download: vi.fn(), list: vi.fn() };
  const messages = { add: vi.fn() };
  const completedBatch = {
    id: 'export-1',
    format: 'CSV',
    status: 'COMPLETED',
    rowCount: 2,
    createdAt: '2026-08-12T11:59:00Z',
    startedAt: '2026-08-12T11:59:30Z',
    finishedAt: '2026-08-12T12:00:00Z',
    firstDownloadedAt: null,
    downloadExpiresAt: null,
    fileDeletedAt: null,
    downloadAvailable: true,
  } as const;
  let exportEvents: Subject<unknown>;

  beforeEach(async () => {
    vi.clearAllMocks();
    walletItems.getAllItems.mockResolvedValue({ content: [] });
    groupWalletItems.getAllItems.mockResolvedValue({ content: [] });
    categories.getAllCategories.mockResolvedValue({ content: [] });
    groupCategories.getAllCategories.mockResolvedValue({ content: [] });
    groups.getAllGroups.mockResolvedValue([]);
    exportService.create.mockResolvedValue({ id: 'export-1', status: 'QUEUED', createdAt: '2026-08-12T00:00:00Z' });
    exportService.delete.mockResolvedValue(undefined);
    exportService.download.mockResolvedValue({
      firstDownloadedAt: '2026-08-12T12:00:00Z',
      downloadExpiresAt: '2026-08-12T12:05:00Z',
    });
    exportService.list.mockResolvedValue([]);
    exportEvents = new Subject();

    await TestBed.configureTestingModule({
      imports: [ExportTransactionsPageComponent],
      providers: [
        { provide: WalletItemService, useValue: walletItems },
        { provide: GroupWalletItemService, useValue: groupWalletItems },
        { provide: UserCategoriesService, useValue: categories },
        { provide: GroupCategoriesService, useValue: groupCategories },
        { provide: GroupService, useValue: groups },
        { provide: ExportService, useValue: exportService },
        { provide: UserActionEventService, useValue: { exportBatchAction$: exportEvents } },
        { provide: MessageService, useValue: messages },
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
      ],
    }).compileComponents();
  });

  it('requests personal wallet items and categories in pages of ten with the backend query', async () => {
    const component = TestBed.createComponent(ExportTransactionsPageComponent).componentInstance;

    await component.walletItemsGetter(2, 'nubank');
    await component.categoriesGetter(3, 'mercado');

    expect(walletItems.getAllItems).toHaveBeenCalledWith({ page: 2, size: 10, sort: 'name' }, false, 'nubank');
    expect(categories.getAllCategories).toHaveBeenCalledWith(
      { onlyRoot: false, mountChildren: false, query: 'mercado' },
      { page: 3, size: 10, sort: 'name' },
    );
  });

  it('uses the paged group endpoints after changing scope', async () => {
    const component = TestBed.createComponent(ExportTransactionsPageComponent).componentInstance;
    await component.groupChanged('group-1');

    await component.walletItemsGetter(1, 'conta');
    await component.categoriesGetter(1, 'casa');

    expect(groupWalletItems.getAllItems).toHaveBeenCalledWith('group-1', { page: 1, size: 10, sort: 'name' }, false, 'conta');
    expect(groupCategories.getAllCategories).toHaveBeenCalledWith(
      'group-1',
      { onlyRoot: false, mountChildren: false, query: 'casa' },
      { page: 1, size: 10, sort: 'name' },
    );
  });

  it('submits revenue expense and transfer as entry types instead of payment types', async () => {
    const component = TestBed.createComponent(ExportTransactionsPageComponent).componentInstance;
    component.form.patchValue({ entryTypes: ['REVENUE', 'EXPENSE', 'TRANSFER'] });

    await component.submit();

    expect(exportService.create).toHaveBeenCalledWith(
      expect.objectContaining({
        filter: expect.objectContaining({ entryTypes: ['REVENUE', 'EXPENSE', 'TRANSFER'], confirmed: null }),
      }),
    );
    expect(exportService.create.mock.calls[0][0].filter).not.toHaveProperty('paymentTypes');
  });

  it('removes a manually deleted export from the history immediately', async () => {
    const component = TestBed.createComponent(ExportTransactionsPageComponent).componentInstance;
    const deleted = { id: 'export-1' } as ExportBatchDto;
    const retained = { id: 'export-2' } as ExportBatchDto;
    component.batches = [deleted, retained];

    await component.delete(deleted);

    expect(exportService.delete).toHaveBeenCalledWith('export-1');
    expect(component.batches).toEqual([retained]);
  });

  it('removes an automatically purged export when its delete event arrives', async () => {
    const fixture = TestBed.createComponent(ExportTransactionsPageComponent);
    await fixture.componentInstance.ngOnInit();
    fixture.componentInstance.batches = [{ id: 'export-1' } as ExportBatchDto];

    exportEvents.next({ type: 'DELETE', data: 'export-1' });

    expect(fixture.componentInstance.batches).toEqual([]);
    fixture.destroy();
  });

  it('applies duplicate completion events without polling or downloading', async () => {
    const fixture = TestBed.createComponent(ExportTransactionsPageComponent);
    await fixture.componentInstance.ngOnInit();

    exportEvents.next({ type: 'UPDATE', data: completedBatch });
    exportEvents.next({ type: 'UPDATE', data: completedBatch });
    expect(exportService.download).not.toHaveBeenCalled();
    expect(fixture.componentInstance.batches).toEqual([expect.objectContaining({ id: 'export-1', status: 'COMPLETED' })]);
    fixture.destroy();
  });

  it('downloads only after an explicit history action', async () => {
    const component = TestBed.createComponent(ExportTransactionsPageComponent).componentInstance;
    component.batches = [completedBatch as ExportBatchDto];

    await component.download(completedBatch as ExportBatchDto);

    expect(exportService.download).toHaveBeenCalledTimes(1);
    expect(component.batches).toEqual([
      expect.objectContaining({
        id: 'export-1',
        firstDownloadedAt: '2026-08-12T12:00:00Z',
        downloadExpiresAt: '2026-08-12T12:05:00Z',
      }),
    ]);
  });
});

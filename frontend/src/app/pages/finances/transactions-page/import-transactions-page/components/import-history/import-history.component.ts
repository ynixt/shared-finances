import { Component, DestroyRef, OnInit, effect, inject, input, output } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialog } from 'primeng/confirmdialog';

import {
  ImportBatchDto,
  ImportBatchStatusEventDto,
} from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/imports';
import { ActionEventType__Obj } from '../../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { ImportService } from '../../../../services/import.service';
import { UserActionEventService } from '../../../../services/user-action-event.service';
import { ImportBatchRemovedEvent } from '../../import-transactions.models';
import { ImportHistoryTableComponent } from './import-history-table.component';
import { canUndoImportBatch, isTerminalImportBatchStatus, mergeImportBatch } from './import-history.lifecycle';

@Component({
  selector: 'app-import-history',
  imports: [TranslatePipe, ConfirmDialog, ImportHistoryTableComponent],
  templateUrl: './import-history.component.html',
  styleUrl: './import-history.component.scss',
})
export class ImportHistoryComponent implements OnInit {
  private readonly importService = inject(ImportService);
  private readonly userActionEventService = inject(UserActionEventService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translateService = inject(TranslateService);

  readonly acceptedBatch = input<ImportBatchDto>();
  readonly batchRemoved = output<ImportBatchRemovedEvent>();

  history: ImportBatchDto[] = [];
  loading = true;
  undoingBatchId?: string;

  constructor() {
    effect(() => {
      const batch = this.acceptedBatch();
      if (batch != null) this.mergeBatch(batch);
    });
  }

  ngOnInit(): void {
    this.userActionEventService.importBatchAction$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(event => {
      if (event.type === ActionEventType__Obj.DELETE) {
        const id = typeof event.data === 'string' ? event.data : (event.data as { id?: string })?.id;
        if (id != null) this.applyBatchDeletedEvent(id);
        return;
      }
      const statusEvent = event.data as ImportBatchStatusEventDto;
      if (statusEvent?.id != null) this.applyBatchStatusEvent(statusEvent);
    });
    this.userActionEventService.resyncRequired$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => void this.reloadHistory());
    void this.reloadHistory();
  }

  confirmUndo(batch: ImportBatchDto): void {
    if (this.undoingBatchId != null || !canUndoImportBatch(batch)) return;
    this.confirmationService.confirm({
      header: this.importText('undoConfirmation.title'),
      message: this.importText('undoConfirmation.message', { count: batch.qty }),
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.importText('actions.undo'),
      rejectLabel: this.translateService.instant('general.cancel'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => void this.undo(batch),
    });
  }

  private async undo(batch: ImportBatchDto): Promise<void> {
    if (this.undoingBatchId != null || !canUndoImportBatch(batch)) return;
    this.undoingBatchId = batch.id;
    try {
      const accepted = await this.importService.undo(batch.id);
      this.mergeBatch(accepted);
      this.messageService.add({
        severity: 'info',
        summary: this.importText('notifications.undoStarted.summary'),
        detail: this.importText('notifications.undoStarted.detail'),
      });
    } catch {
      this.messageService.add({
        severity: 'error',
        summary: this.importText('notifications.undoFailed.summary'),
        detail: this.importText('notifications.undoFailed.retry'),
      });
    } finally {
      this.undoingBatchId = undefined;
    }
  }

  private applyBatchStatusEvent(event: ImportBatchStatusEventDto): void {
    const existing = this.history.find(item => item.id === event.id);
    if (existing == null) {
      void this.reloadHistory();
      return;
    }
    const updated: ImportBatchDto = { ...existing, ...event };
    this.mergeBatch(updated);
    if (isTerminalImportBatchStatus(updated.status)) this.notifyTerminalTransition(updated, existing.status);
  }

  private applyBatchDeletedEvent(batchId: string): void {
    const batch = this.history.find(item => item.id === batchId);
    if (batch == null) return;
    this.history = this.history.filter(item => item.id !== batchId);
    this.messageService.add({
      severity: 'success',
      summary: this.importText('notifications.undoCompleted.summary'),
      detail: this.importText('notifications.undoCompleted.detail'),
    });
    this.batchRemoved.emit({ id: batch.id, fileHash: batch.fileHash });
  }

  private async reloadHistory(): Promise<void> {
    try {
      this.history = await this.importService.list();
    } catch {
      // Keep the last authoritative snapshot visible while the connection recovers.
    } finally {
      this.loading = false;
    }
  }

  private mergeBatch(batch: ImportBatchDto): void {
    this.history = mergeImportBatch(this.history, batch);
  }

  private notifyTerminalTransition(batch: ImportBatchDto, previousStatus?: ImportBatchDto['status']): void {
    if (previousStatus === batch.status) return;
    if (batch.status === 'COMPLETED') {
      this.messageService.add({
        severity: 'success',
        summary: this.importText('notifications.importCompleted.summary'),
        detail: this.importText('notifications.importCompleted.detail', { count: batch.qty }),
      });
    } else if (batch.status === 'FAILED') {
      this.messageService.add({
        severity: 'error',
        summary: this.importText('notifications.importFailed.summary'),
        detail: batch.errorMessage ?? this.importText('notifications.importFailed.retry'),
      });
    } else if (batch.status === 'UNDO_FAILED') {
      this.messageService.add({
        severity: 'error',
        summary: this.importText('notifications.undoFailed.summary'),
        detail: batch.errorMessage ?? this.importText('notifications.undoFailed.retryImport'),
      });
    }
  }

  private importText(key: string, params?: Record<string, unknown>): string {
    return this.translateService.instant(`financesPage.transactionsPage.importPage.${key}`, params);
  }
}

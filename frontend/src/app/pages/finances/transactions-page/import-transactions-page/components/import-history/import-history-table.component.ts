import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonDirective } from 'primeng/button';
import { DataView } from 'primeng/dataview';

import { ImportBatchDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/imports';
import { canUndoImportBatch, hasCompletedImportBatch, importBatchStatusLabelKey } from './import-history.lifecycle';

@Component({
  selector: 'app-import-history-table',
  imports: [CommonModule, TranslatePipe, ButtonDirective, DataView],
  templateUrl: './import-history-table.component.html',
  styleUrl: './import-history-table.component.scss',
})
export class ImportHistoryTableComponent {
  readonly batches = input.required<ImportBatchDto[]>();
  readonly undoingBatchId = input<string>();
  readonly undoRequested = output<ImportBatchDto>();

  statusLabelKey(batch: ImportBatchDto): string {
    return importBatchStatusLabelKey(batch.status);
  }

  canUndo(batch: ImportBatchDto): boolean {
    return canUndoImportBatch(batch);
  }

  isCompleted(batch: ImportBatchDto): boolean {
    return hasCompletedImportBatch(batch);
  }
}

import { Injectable, inject } from '@angular/core';

import { MessageService } from 'primeng/api';

import { ImportService } from '../../services/import.service';
import { ImportPreviewRow } from './import-transactions.models';

@Injectable()
export class CsvImportDuplicateService {
  private readonly importService = inject(ImportService);
  private readonly messageService = inject(MessageService);
  private requestId = 0;

  async refresh(rows: ImportPreviewRow[], autoIgnore: boolean, text: (key: string) => string): Promise<void> {
    const requestId = ++this.requestId;
    rows.forEach(row => (row.duplicate = false));
    const checkableRows = rows.filter(
      (row): row is ImportPreviewRow & { walletItemId: string } => row.walletItemId != null && row.walletItemId !== '',
    );
    if (checkableRows.length === 0) return;
    try {
      const duplicates = await this.importService.checkDuplicates({
        lines: checkableRows.map(row => ({
          walletItemId: row.walletItemId,
          name: row.name,
          value: row.convertedValue ?? 0,
          date: row.date ?? '0001-01-01',
          installment: row.installment?.current,
        })),
      });
      if (requestId !== this.requestId) return;
      duplicates.forEach(index => {
        const row = checkableRows[index];
        if (row != null) {
          row.duplicate = true;
          if (autoIgnore) row.included = false;
        }
      });
    } catch {
      if (requestId === this.requestId) {
        this.messageService.add({
          severity: 'warn',
          summary: text('notifications.duplicates.summary'),
          detail: text('notifications.duplicates.detail'),
        });
      }
    }
  }
}

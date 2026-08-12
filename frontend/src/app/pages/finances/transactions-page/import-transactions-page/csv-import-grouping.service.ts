import { Injectable, inject } from '@angular/core';

import { CsvImportCatalogStore } from './csv-import-catalog.store';
import { ImportPreviewRow } from './import-transactions.models';

@Injectable()
export class CsvImportGroupingService {
  private readonly catalogs = inject(CsvImportCatalogStore);

  apply(rows: ImportPreviewRow[], text: (key: string) => string): void {
    rows.forEach(row => {
      row.previewHidden = false;
      row.transferDisplayName = undefined;
      row.transferPreviewLeaderIndex = undefined;
    });
    const groups = new Map<string, ImportPreviewRow[]>();
    rows.forEach(row => {
      if (row.transferGroupId == null) return;
      groups.set(row.transferGroupId, [...(groups.get(row.transferGroupId) ?? []), row]);
    });
    for (const group of groups.values()) this.applyTransferGroup(group, text);
  }

  private applyTransferGroup(group: ImportPreviewRow[], text: (key: string) => string): void {
    if (group.length === 1) return;
    const negative = group.find(row => (row.value ?? 0) < 0);
    const positive = group.find(row => (row.value ?? 0) > 0);
    if (group.length !== 2 || negative == null || positive == null) {
      group.forEach(row => {
        row.parseError = text('validation.invalidTransferGroup');
        row.included = false;
      });
      return;
    }

    const origin = this.catalogs.originFor(negative)?.name ?? text('preview.unknownOrigin');
    const target = this.catalogs.originFor(positive)?.name ?? text('preview.unknownOrigin');
    negative.transferDisplayName = `${origin} → ${target}`;
    negative.transferPreviewLeaderIndex = negative.index;
    positive.transferPreviewLeaderIndex = negative.index;
    positive.previewHidden = true;
    negative.beneficiaries = [];
    positive.beneficiaries = [];
  }
}

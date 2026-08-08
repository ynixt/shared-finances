import { Injectable, inject } from '@angular/core';

import { MessageService } from 'primeng/api';

import { CreateImportDto, ImportBatchDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/imports';
import { ImportService } from '../../services/import.service';
import { ImportPreviewRow } from './import-transactions.models';

export interface CsvImportSubmissionSnapshot {
  canShowPreview: boolean;
  file?: File;
  fileHash: string;
  rows: ImportPreviewRow[];
}

export type CsvImportSubmissionResult = { batch: ImportBatchDto; errorKey?: never } | { batch?: never; errorKey: string };

@Injectable()
export class CsvImportSubmissionService {
  private readonly importService = inject(ImportService);
  private readonly messageService = inject(MessageService);

  async submit(snapshot: CsvImportSubmissionSnapshot, text: (key: string) => string): Promise<CsvImportSubmissionResult> {
    if (!snapshot.canShowPreview) return { errorKey: 'errors.fixedOriginRequired' };
    if (snapshot.rows.length === 0 || snapshot.file == null) return { errorKey: 'errors.noValidRows' };

    try {
      const batch = await this.importService.create(this.createRequest(snapshot.file, snapshot.fileHash, snapshot.rows));
      this.messageService.add({
        severity: 'info',
        summary: text('notifications.importStarted.summary'),
        detail: text('notifications.importStarted.detail'),
      });
      return { batch };
    } catch {
      return { errorKey: 'errors.startImport' };
    }
  }

  createRequest(file: File, fileHash: string, rows: ImportPreviewRow[]): CreateImportDto {
    return {
      fileHash,
      fileName: file.name,
      format: 'CSV',
      lines: rows.map(row => ({
        walletItemId: row.walletItemId!,
        name: row.name,
        value: row.convertedValue!,
        date: row.date!,
        confirmed: row.confirmed,
        categoryId: row.categoryId,
        groupId: row.groupId,
        beneficiaries:
          row.beneficiaries.length === 0
            ? undefined
            : row.beneficiaries.map(leg => ({ userId: leg.userId, benefitPercent: leg.benefitPercent })),
        billDate: row.billDate,
        installment: row.installment?.current,
        installmentTotal: row.installment?.total,
        createPreviousInstallments: row.createPreviousInstallments,
        createFollowingInstallments: row.createFollowingInstallments,
        tags: row.tags,
        observations: row.observations,
      })),
    };
  }
}

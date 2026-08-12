/* eslint-disable */
/* tslint-disable */

import { ExportBatchStatus } from '../../../../domain/enums/export-batch-status';
import { ExportFormat } from '../../../../domain/enums/export-format';

export interface ExportBatchStatusEventDto {
  createdAt: any;
  downloadAvailable: boolean;
  downloadExpiresAt?: any | null;
  errorMessage?: string | null;
  fileDeletedAt?: any | null;
  finishedAt?: any | null;
  firstDownloadedAt?: any | null;
  format: ExportFormat;
  id: string;
  rowCount?: number | null;
  startedAt?: any | null;
  status: ExportBatchStatus;
}

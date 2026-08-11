/* eslint-disable */
/* tslint-disable */

import { ImportBatchStatus } from '../../../../domain/enums/import-batch-status';

export interface ImportBatchDto {
  createdAt: any;
  errorMessage?: string | null;
  fileHash: string;
  fileName: string;
  finishedAt?: any | null;
  format: string;
  id: string;
  qty: number;
  retries: number;
  startedAt?: any | null;
  status: ImportBatchStatus;
  totalCredit: number;
  totalDebit: number;
  walletItemId?: string | null;
  walletItemName: string;
}

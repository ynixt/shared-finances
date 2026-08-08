/* eslint-disable */
/* tslint-disable */
import { ImportBatchStatus } from '../../../../domain/enums/import-batch-status';

export interface ImportBatchStatusEventDto {
  errorMessage?: string | null;
  finishedAt?: any | null;
  id: string;
  retries: number;
  startedAt?: any | null;
  status: ImportBatchStatus;
}

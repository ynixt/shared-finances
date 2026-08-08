/* eslint-disable */
/* tslint-disable */
import { ImportHashStatus } from '../../../../domain/enums/import-hash-status';

export interface ImportHashCheckDto {
  batchId?: string | null;
  fileName?: string | null;
  importedAt?: any | null;
  status: ImportHashStatus;
}

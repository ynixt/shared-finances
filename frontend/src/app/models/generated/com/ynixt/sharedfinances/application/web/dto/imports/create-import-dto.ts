/* eslint-disable */
/* tslint-disable */
import { ImportLineDto } from './import-line-dto';

export interface CreateImportDto {
  fileHash: string;
  fileName: string;
  format: string;
  lines: Array<ImportLineDto>;
}

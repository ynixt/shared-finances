/* eslint-disable */
/* tslint-disable */

import { ExportFormat } from '../../../../domain/enums/export-format';
import { TransactionExportFilterDto } from './transaction-export-filter-dto';

export interface CreateExportDto {
  filter: TransactionExportFilterDto;
  format: ExportFormat;
}

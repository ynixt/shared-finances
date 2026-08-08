import { Injectable } from '@angular/core';

import { CSV_IMPORT_DATE_FORMATS } from './csv-import-draft.config';
import { CsvImportSourceAdapter } from './csv-import-source.adapter';
import { CsvDateFormat, ParsedCsv } from './csv-statement-parser';
import { decodeCsvBytes } from './import-file-source';

@Injectable()
export class CsvImportDraftStore {
  private readonly source = new CsvImportSourceAdapter();

  readonly dateFormats = CSV_IMPORT_DATE_FORMATS;
  delimiter = ';';
  decimalSeparator: '.' | ',' = '.';
  dateFormat: CsvDateFormat = 'AUTO';
  detectedDateFormat: Exclude<CsvDateFormat, 'AUTO'> = 'DD/MM/YYYY';
  detectedLayoutProviderId?: string;
  invertValues = false;
  separateCreditDebit = false;
  headers: string[] = [];
  fileText = '';

  load(bytes: ArrayBuffer): void {
    this.fileText = decodeCsvBytes(bytes);
  }

  parse(): ParsedCsv {
    return this.source.parse(this.fileText, {
      delimiter: this.delimiter,
      decimalSeparator: this.decimalSeparator,
      dateFormat: this.dateFormat,
    });
  }

  reset(): void {
    this.fileText = '';
    this.headers = [];
    this.detectedLayoutProviderId = undefined;
  }
}

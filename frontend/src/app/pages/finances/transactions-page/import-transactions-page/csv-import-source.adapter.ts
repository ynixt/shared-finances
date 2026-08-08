import { CsvParseOptions, ParsedCsv, parseCsv } from './csv-statement-parser';

export class CsvImportSourceAdapter {
  readonly format = 'CSV' as const;

  parse(text: string, options: CsvParseOptions): ParsedCsv {
    return parseCsv(text, options);
  }
}

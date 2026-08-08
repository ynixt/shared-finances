import dayjs from 'dayjs';

import { ImportHashCheckDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/imports';
import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { CsvColumnField, CsvColumnMapping, CsvDateFormat, normalizeHeader } from './csv-statement-parser';
import { ImportFileFormat, ParsedImportSourceStatement } from './import-file-source';
import { FixedValue, ImportPreviewRow } from './import-transactions.models';

export abstract class ImportDraftState {
  fileFormat?: ImportFileFormat;
  delimiter = ';';
  decimalSeparator: '.' | ',' = '.';
  dateFormat: CsvDateFormat = 'AUTO';
  detectedDateFormat: Exclude<CsvDateFormat, 'AUTO'> = 'DD/MM/YYYY';
  detectedLayoutProviderId?: string;
  invertValues = false;
  separateCreditDebit = false;
  autoIgnoreDuplicates = false;
  search = '';
  categoryFilter = '';
  mapping: CsvColumnMapping = {};
  fixedValues: Partial<Record<CsvColumnField, FixedValue>> = { origin: '', bill: dayjs().format('YYYY-MM') };
  fixedCategory?: CategoryDto;
  headers: string[] = [];
  rows: ImportPreviewRow[] = [];
  hashCheck: ImportHashCheckDto | null = null;
  file?: File;
  fileHash = '';
  fileText = '';
  ofxStatements: ParsedImportSourceStatement[] = [];
  ofxStatementOrigins: Record<string, string> = {};
  ofxPendingCount = 0;
  maxLines = 1000;
  importPreferencesLoaded = false;
  loading = true;
  parsing = false;
  submitting = false;
  error?: string;
  previewFirst = 0;
  previewPageSize = 20;

  abstract get canShowPreview(): boolean;
  abstract displayCurrencyFor(row: ImportPreviewRow): string;
  abstract hasValidBeneficiaries(row: ImportPreviewRow): boolean;

  get hasParsedRows(): boolean {
    return this.rows.length > 0;
  }

  get visibleRows(): ImportPreviewRow[] {
    const normalizedSearch = normalizeHeader(this.search);
    return this.rows.filter(row => {
      const matchesSearch =
        normalizedSearch === '' ||
        normalizeHeader(row.name ?? '').includes(normalizedSearch) ||
        Object.values(row.raw).some(value => normalizeHeader(value).includes(normalizedSearch));
      return matchesSearch && (this.categoryFilter === '' || row.categoryId === this.categoryFilter);
    });
  }

  get pagedRows(): ImportPreviewRow[] {
    return this.visibleRows.slice(this.previewFirst, this.previewFirst + this.previewPageSize);
  }

  get selectedRows(): ImportPreviewRow[] {
    return this.rows.filter(
      row =>
        row.included &&
        row.parseError == null &&
        row.walletItemId != null &&
        row.date != null &&
        row.value != null &&
        row.convertedValue != null &&
        this.hasValidBeneficiaries(row),
    );
  }

  get revenueCount(): number {
    return this.selectedRows.filter(row => (row.convertedValue ?? 0) >= 0).length;
  }

  get expenseCount(): number {
    return this.selectedRows.filter(row => (row.convertedValue ?? 0) < 0).length;
  }

  get balanceImpacts(): Array<{ currency: string; value: number }> {
    const impacts = new Map<string, number>();
    this.selectedRows.forEach(row => {
      const currency = this.displayCurrencyFor(row);
      impacts.set(currency, (impacts.get(currency) ?? 0) + (row.convertedValue ?? 0));
    });
    return Array.from(impacts, ([currency, value]) => ({ currency, value }));
  }

  get canSubmit(): boolean {
    return this.canShowPreview && this.selectedRows.length > 0 && this.file != null && !this.submitting;
  }

  resetPreviewPagination(): void {
    this.previewFirst = 0;
  }

  previewPageChanged(event: { first?: number; rows?: number }): void {
    this.previewFirst = event.first ?? 0;
    this.previewPageSize = event.rows ?? 20;
  }

  setAutoIgnoreDuplicates(enabled: boolean): void {
    this.autoIgnoreDuplicates = enabled;
    if (enabled) this.rows.filter(row => row.duplicate).forEach(row => (row.included = false));
  }
}

export { ImportDraftState as CsvImportDraftState };

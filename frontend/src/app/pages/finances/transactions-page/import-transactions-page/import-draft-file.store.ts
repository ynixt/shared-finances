import { inject } from '@angular/core';

import dayjs from 'dayjs';

import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { WalletItemSearchResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { ImportService } from '../../services/import.service';
import { CsvImportCatalogStore } from './csv-import-catalog.store';
import { CsvImportConversionService } from './csv-import-conversion.service';
import { CsvImportDraftStore } from './csv-import-draft.store';
import { CsvImportDuplicateService } from './csv-import-duplicate.service';
import { CsvImportGroupingService } from './csv-import-grouping.service';
import { CsvImportRowContext, CsvImportRowResolver } from './csv-import-row.resolver';
import { CsvColumnField, detectDateFormat } from './csv-statement-parser';
import { ImportDraftState } from './import-draft.state';
import { detectImportFileFormat, sha256Bytes } from './import-file-source';
import { MappingOption } from './import-transactions.models';
import { parseImportTags } from './import-transactions.utils';
import { sharedFinancesCsvTemplateV1LayoutProvider } from './layout-providers/shared-finances-csv-template-v1.layout-provider';
import { OfxImportDraftStore } from './ofx-import-draft.store';
import { OfxParseError } from './ofx-statement-parser';

export abstract class ImportDraftFileStore extends ImportDraftState {
  protected readonly csvStore = inject(CsvImportDraftStore);
  protected readonly ofxStore = inject(OfxImportDraftStore);
  protected readonly catalogs = inject(CsvImportCatalogStore);
  protected readonly rowResolver = inject(CsvImportRowResolver);
  protected readonly conversions = inject(CsvImportConversionService);
  protected readonly duplicates = inject(CsvImportDuplicateService);
  protected readonly grouping = inject(CsvImportGroupingService);
  protected readonly importService = inject(ImportService);

  abstract readonly mappingOptions: MappingOption[];
  abstract readonly billFromDateMappingValue: string;
  abstract readonly fixedMappingValue: string;
  protected abstract get rowContext(): CsvImportRowContext;
  protected abstract refreshConversions(): Promise<void>;
  protected abstract refreshDuplicates(): Promise<void>;
  protected abstract importText(key: string, params?: Record<string, unknown>): string;

  get defaultCurrency(): string {
    return this.catalogs.defaultCurrency;
  }
  set defaultCurrency(value: string) {
    this.catalogs.defaultCurrency = value;
  }
  set currencyOptions(value: string[]) {
    this.catalogs.currencyOptions = value;
  }
  get walletItems(): WalletItemSearchResponseDto[] {
    return this.catalogs.walletItems;
  }
  set walletItems(value: WalletItemSearchResponseDto[]) {
    this.catalogs.walletItems = value;
  }
  get categories(): CategoryDto[] {
    return this.catalogs.categories;
  }
  set categories(value: CategoryDto[]) {
    this.catalogs.categories = value;
  }
  get groups(): GroupWithRoleDto[] {
    return this.catalogs.groups;
  }
  set groups(value: GroupWithRoleDto[]) {
    this.catalogs.groups = value;
  }

  async initialize(): Promise<void> {
    try {
      const [, preferences] = await Promise.all([this.catalogs.load(), this.importService.preferences()]);
      this.maxLines = preferences.maxLines ?? null;
      this.importPreferencesLoaded = true;
    } catch {
      this.error = this.importText('errors.loadData');
    } finally {
      this.loading = false;
    }
  }

  async selectFile(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file == null || !this.importPreferencesLoaded) return;
    this.parsing = true;
    this.error = undefined;
    try {
      this.file = file;
      const bytes = await file.arrayBuffer();
      this.fileFormat = detectImportFileFormat(bytes, file.name);
      this.fileHash = await sha256Bytes(bytes);
      const hash = await this.importService.checkHash(this.fileHash);
      this.hashCheck = hash.status === 'NOT_IMPORTED' ? null : hash;
      if (this.fileFormat === 'OFX') {
        this.ofxStore.parse(bytes, this.maxLines ?? Number.MAX_SAFE_INTEGER);
        this.mapping = {
          bill: this.billFromDateMappingValue,
          category: this.fixedMappingValue,
          confirmed: this.fixedMappingValue,
          group: this.fixedMappingValue,
          tags: this.fixedMappingValue,
        };
        this.fixedValues = { bill: dayjs().format('YYYY-MM'), category: '', confirmed: true, group: '', tags: '' };
        await this.reprocessOfx(true);
      } else {
        this.csvStore.load(bytes);
        await this.reprocessCsv(true);
      }
    } catch (error) {
      const message = this.fileErrorMessage(error);
      this.removeFile();
      this.error = message;
    } finally {
      this.parsing = false;
    }
  }

  removeFile(): void {
    this.file = undefined;
    this.fileHash = '';
    this.csvStore.reset();
    this.fileFormat = undefined;
    this.ofxStore.reset();
    this.hashCheck = null;
    this.mapping = {};
    this.fixedValues = { origin: '', bill: dayjs().format('YYYY-MM') };
    this.fixedCategory = undefined;
    this.rows = [];
    this.resetPreviewPagination();
    this.error = undefined;
  }

  async reprocess(resetMapping = false): Promise<void> {
    if (this.fileFormat === 'OFX') return this.reprocessOfx(resetMapping);
    return this.reprocessCsv(resetMapping);
  }

  private async reprocessCsv(resetMapping = false): Promise<void> {
    if (this.csvStore.fileText === '') return;
    try {
      const inclusionByIndex = new Map(this.rows.map(row => [row.index, row.included] as const));
      const parsed = this.csvStore.parse();
      if (this.maxLines != null && parsed.rows.length > this.maxLines) {
        const maxLines = this.maxLines;
        this.removeFile();
        this.error = this.importText('errors.lineLimitExceeded', { maxLines });
        return;
      }
      this.csvStore.headers = parsed.headers;
      this.csvStore.detectedLayoutProviderId = parsed.layoutProviderId;
      const detected = parsed.mapping;
      const detectedOrigin =
        parsed.layoutProviderId === sharedFinancesCsvTemplateV1LayoutProvider.id ? detected.origin : this.fixedMappingValue;
      this.mapping = resetMapping
        ? { ...detected, origin: detectedOrigin, bill: this.billFromDateMappingValue }
        : Object.fromEntries(
            this.mappingOptions
              .map(({ field }) => [
                field,
                this.mapping[field] === this.fixedMappingValue ||
                (field === 'bill' && this.mapping[field] === this.billFromDateMappingValue) ||
                (this.mapping[field] && parsed.headers.includes(this.mapping[field]!))
                  ? this.mapping[field]
                  : field === 'origin' || field === 'bill'
                    ? field === 'bill'
                      ? this.billFromDateMappingValue
                      : this.fixedMappingValue
                    : detected[field],
              ])
              .filter(([, value]) => value != null),
          );
      this.csvStore.detectedDateFormat =
        this.csvStore.dateFormat === 'AUTO' && this.mapping.date != null
          ? detectDateFormat(parsed.rows.map(row => row[this.mapping.date!] ?? '').filter(Boolean))
          : parsed.detectedDateFormat;
      this.rows = parsed.rows.map((raw, index) => this.rowResolver.create(raw, index, this.rowContext));
      if (!resetMapping) this.rows.forEach(row => (row.included = inclusionByIndex.get(row.index) ?? row.included));
      await this.resolveRows();
      this.error = undefined;
    } catch (error) {
      this.rows = [];
      this.error = error instanceof Error ? error.message : this.importText('errors.parseCsv');
    }
  }

  private async reprocessOfx(resetRows = false): Promise<void> {
    if (this.ofxStore.statements.length === 0) return;
    const inclusionByIndex = new Map(this.rows.map(row => [row.index, row.included] as const));
    const sources = this.ofxStore.statements.flatMap(statement => statement.rows);
    this.rows = sources.map((source, index) => {
      const row = this.rowResolver.createFromSource(
        source,
        index,
        source.sourceStatementKey == null ? undefined : this.ofxStore.statementOrigins[source.sourceStatementKey],
        this.rowContext,
      );
      row.confirmed = this.fixedValues.confirmed !== false;
      row.tags = parseImportTags(String(this.fixedValues.tags ?? ''));
      if (!resetRows) row.included = inclusionByIndex.get(index) ?? row.included;
      return row;
    });
    await this.resolveRows();
    this.error = undefined;
  }

  private async resolveRows(): Promise<void> {
    this.resetPreviewPagination();
    await this.rowResolver.resolve(this.rows, this.rowContext);
    this.grouping.apply(this.rows, key => this.importText(key));
    this.rowResolver.applyBillSuggestions(this.rows, this.rowContext);
    await this.refreshConversions();
    await this.refreshDuplicates();
  }

  private fileErrorMessage(error: unknown): string {
    if (error instanceof OfxParseError) {
      if (error.code === 'lineLimitExceeded') return this.importText('errors.lineLimitExceeded', error.params);
      return this.importText(`errors.ofx.${error.code}`, error.params);
    }
    return error instanceof Error ? error.message : this.importText('errors.readFile');
  }
}

import { Injectable, inject } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';

import dayjs from 'dayjs';

import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { ImportBatchDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/imports';
import { WalletItemSearchResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { ImportService } from '../../services/import.service';
import { UserForBeneficiary } from '../shared/transaction-form/transaction-form.types';
import { CsvImportBeneficiaryEditor } from './csv-import-beneficiary.editor';
import { CsvImportCatalogStore } from './csv-import-catalog.store';
import { CsvImportConversionContext, CsvImportConversionService } from './csv-import-conversion.service';
import {
  CSV_IMPORT_DATE_FORMATS,
  CSV_IMPORT_FIXED_MAPPING_VALUE,
  CSV_IMPORT_MAPPING_OPTIONS,
  CSV_IMPORT_PREVIEW_PAGE_SIZE_OPTIONS,
} from './csv-import-draft.config';
import { CsvImportDraftState } from './csv-import-draft.state';
import { CsvImportDuplicateService } from './csv-import-duplicate.service';
import { CsvImportRowContext, CsvImportRowResolver } from './csv-import-row.resolver';
import { CsvImportSubmissionService } from './csv-import-submission.service';
import { CsvColumnField, detectDateFormat, parseCsv, parseCsvDate, parseInstallment, sha256File } from './csv-statement-parser';
import { FixedValue, ImportPreviewRow, MappingOption } from './import-transactions.models';
import { formatImportInstallment, formatImportTags, hasValidImportBeneficiaries, parseImportTags } from './import-transactions.utils';

@Injectable()
export class CsvImportDraftStore extends CsvImportDraftState {
  private readonly catalogs = inject(CsvImportCatalogStore);
  private readonly rowResolver = inject(CsvImportRowResolver);
  private readonly conversions = inject(CsvImportConversionService);
  private readonly duplicates = inject(CsvImportDuplicateService);
  private readonly beneficiaryEditor = inject(CsvImportBeneficiaryEditor);
  private readonly submission = inject(CsvImportSubmissionService);
  private readonly importService = inject(ImportService);
  private readonly translateService = inject(TranslateService);

  readonly dateFormats = CSV_IMPORT_DATE_FORMATS;
  readonly mappingOptions: MappingOption[] = CSV_IMPORT_MAPPING_OPTIONS;
  readonly fixedMappingValue = CSV_IMPORT_FIXED_MAPPING_VALUE;
  readonly previewPageSizeOptions = CSV_IMPORT_PREVIEW_PAGE_SIZE_OPTIONS;
  get beneficiaryDialogVisible(): boolean {
    return this.beneficiaryEditor.visible;
  }

  set beneficiaryDialogVisible(value: boolean) {
    this.beneficiaryEditor.visible = value;
  }

  get beneficiaryMembers(): UserForBeneficiary[] {
    return this.beneficiaryEditor.members;
  }

  get beneficiaryForm(): FormGroup {
    return this.beneficiaryEditor.form;
  }

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

  private get rowContext(): CsvImportRowContext {
    return {
      dateFormat: this.dateFormat,
      detectedDateFormat: this.detectedDateFormat,
      decimalSeparator: this.decimalSeparator,
      separateCreditDebit: this.separateCreditDebit,
      invertValues: this.invertValues,
      mapping: this.mapping,
      fixedMappingValue: this.fixedMappingValue,
      fixedValues: this.fixedValues,
      fixedCategory: this.fixedCategory,
      text: (key, params) => this.importText(key, params),
    };
  }

  private get conversionContext(): CsvImportConversionContext {
    return {
      displayCurrency: row => this.displayCurrencyFor(row),
      text: (key, params) => this.importText(key, params),
    };
  }

  async initialize(): Promise<void> {
    try {
      const [, preferences] = await Promise.all([this.catalogs.load(), this.importService.preferences()]);
      this.maxLines = preferences.maxLines;
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
      [this.fileText, this.fileHash] = await Promise.all([file.text(), sha256File(file)]);
      const hash = await this.importService.checkHash(this.fileHash);
      this.hashCheck = hash.status === 'NOT_IMPORTED' ? null : hash;
      await this.reprocess(true);
    } catch (error) {
      this.removeFile();
      this.error = error instanceof Error ? error.message : this.importText('errors.readFile');
    } finally {
      this.parsing = false;
    }
  }

  removeFile(): void {
    this.file = undefined;
    this.fileHash = '';
    this.fileText = '';
    this.hashCheck = null;
    this.headers = [];
    this.detectedLayoutProviderId = undefined;
    this.mapping = {};
    this.fixedValues = { origin: '', bill: dayjs().format('YYYY-MM') };
    this.fixedCategory = undefined;
    this.rows = [];
    this.resetPreviewPagination();
    this.error = undefined;
  }

  async reprocess(resetMapping = false): Promise<void> {
    if (this.fileText === '') return;
    try {
      const inclusionByIndex = new Map(this.rows.map(row => [row.index, row.included] as const));
      const parsed = parseCsv(this.fileText, {
        delimiter: this.delimiter,
        decimalSeparator: this.decimalSeparator,
        dateFormat: this.dateFormat,
      });
      if (parsed.rows.length > this.maxLines) {
        const maxLines = this.maxLines;
        this.removeFile();
        this.error = this.importText('errors.lineLimitExceeded', { maxLines });
        return;
      }
      this.headers = parsed.headers;
      this.detectedLayoutProviderId = parsed.layoutProviderId;
      const detected = parsed.mapping;
      this.mapping = resetMapping
        ? { ...detected, origin: this.fixedMappingValue, bill: this.fixedMappingValue }
        : Object.fromEntries(
            this.mappingOptions
              .map(({ field }) => [
                field,
                this.mapping[field] === this.fixedMappingValue || (this.mapping[field] && parsed.headers.includes(this.mapping[field]!))
                  ? this.mapping[field]
                  : field === 'origin' || field === 'bill'
                    ? this.fixedMappingValue
                    : detected[field],
              ])
              .filter(([, value]) => value != null),
          );
      this.detectedDateFormat =
        this.dateFormat === 'AUTO' && this.mapping.date != null
          ? detectDateFormat(parsed.rows.map(row => row[this.mapping.date!] ?? '').filter(Boolean))
          : parsed.detectedDateFormat;
      this.rows = parsed.rows.map((raw, index) => this.rowResolver.create(raw, index, this.rowContext));
      if (!resetMapping) {
        this.rows.forEach(row => {
          row.included = inclusionByIndex.get(row.index) ?? row.included;
        });
      }
      this.resetPreviewPagination();
      await this.rowResolver.resolve(this.rows, this.rowContext);
      this.rowResolver.applyBillSuggestions(this.rows, this.rowContext);
      await this.refreshConversions();
      await this.refreshDuplicates();
      this.error = undefined;
    } catch (error) {
      this.rows = [];
      this.error = error instanceof Error ? error.message : this.importText('errors.parseCsv');
    }
  }

  async setMapping(field: CsvColumnField, column: string): Promise<void> {
    const previousFixedCategory = this.fixedCategory;
    if ((field === 'origin' || field === 'bill') && column === '') {
      this.mapping[field] = this.fixedMappingValue;
    } else if (column === '') {
      delete this.mapping[field];
    } else {
      this.mapping[field] = column;
    }
    if (this.mapping[field] === this.fixedMappingValue) {
      this.initializeFixedValue(field);
    }
    if (field === 'group' && this.isFixedMapping('category') && previousFixedCategory != null) {
      this.fixedCategory =
        this.fixedCategories.find(category => category.conceptId === previousFixedCategory.conceptId) ??
        this.catalogs.findCategoryByName(this.fixedCategories, previousFixedCategory.name);
      this.fixedValues.category = this.fixedCategory?.id ?? '';
    }
    await this.reprocess();
  }

  get fixedMappingOptions(): MappingOption[] {
    return this.mappingOptions.filter(option => this.isMappingOptionVisible(option.field) && this.isFixedMapping(option.field));
  }

  isMappingOptionVisible(field: CsvColumnField): boolean {
    return (
      (field !== 'value' || !this.separateCreditDebit) &&
      (field !== 'credit' || this.separateCreditDebit) &&
      (field !== 'debit' || this.separateCreditDebit)
    );
  }

  isFixedMapping(field: CsvColumnField): boolean {
    return this.mapping[field] === this.fixedMappingValue;
  }

  get fixedCategories(): CategoryDto[] {
    return this.catalogs.categoriesFor(this.fixedGroupId);
  }

  get fixedGroupId(): string | undefined {
    if (!this.isFixedMapping('group')) return undefined;
    const groupId = String(this.fixedValues.group ?? '');
    return groupId === '' ? undefined : groupId;
  }

  get fixedOriginId(): string | undefined {
    if (!this.isFixedMapping('origin')) return undefined;
    const originId = String(this.fixedValues.origin ?? '');
    return this.walletItems.some(item => item.id === originId) ? originId : undefined;
  }

  get fixedOrigin(): WalletItemSearchResponseDto | undefined {
    return this.walletItems.find(item => item.id === this.fixedOriginId);
  }

  get canShowPreview(): boolean {
    return !this.isFixedMapping('origin') || this.fixedOrigin != null;
  }

  async setFixedValue(field: CsvColumnField, value: FixedValue | null | undefined): Promise<void> {
    const normalizedValue = value == null ? '' : value;
    if (this.fixedValues[field] === normalizedValue) return;

    if (field === 'group') {
      const previousCategory = this.fixedCategory;
      this.fixedValues[field] = normalizedValue;
      const groupId = String(normalizedValue);
      if (groupId !== '') {
        await Promise.all([this.catalogs.ensureGroupCategories(groupId), this.catalogs.ensureGroupMembers(groupId)]);
      }
      this.fixedCategory =
        previousCategory == null
          ? undefined
          : (this.fixedCategories.find(category => category.conceptId === previousCategory.conceptId) ??
            this.catalogs.findCategoryByName(this.fixedCategories, previousCategory.name));
      this.fixedValues.category = this.fixedCategory?.id ?? '';
    } else {
      this.fixedValues[field] = normalizedValue;
    }

    await this.reprocess();
  }

  async fixedCategoryChanged(category: CategoryDto | null | undefined): Promise<void> {
    this.fixedCategory = category ?? undefined;
    this.fixedValues.category = category?.id ?? '';
    await this.reprocess();
  }

  async rowOriginChanged(row: ImportPreviewRow): Promise<void> {
    row.walletItemId = this.walletItems.some(item => item.id === row.walletItemId) ? row.walletItemId : undefined;
    this.rowResolver.applyBillSuggestion(row, this.rowContext);
    this.resetRowConversion(row);
    await this.refreshConversions();
    await this.refreshDuplicates();
  }

  async rowCurrencyChanged(row: ImportPreviewRow): Promise<void> {
    row.currency = this.catalogs.findKnownCurrency(row.currency);
    row.currencySource = 'MANUAL';
    this.resetRowConversion(row);
    await this.refreshConversions();
    await this.refreshDuplicates();
  }

  async convertedValueChanged(row: ImportPreviewRow): Promise<void> {
    const rawValue = row.convertedValue as number | string | null | undefined;
    const convertedValue = Number(rawValue);
    if (rawValue != null && rawValue !== '' && Number.isFinite(convertedValue)) {
      row.convertedValue = convertedValue;
      row.convertedValueOverridden = true;
      row.conversionTargetCurrency = this.displayCurrencyFor(row);
      row.conversionError = undefined;
    } else {
      row.convertedValue = undefined;
      row.convertedValueOverridden = false;
      await this.refreshConversions();
    }
    await this.refreshDuplicates();
  }

  async rowDateChanged(row: ImportPreviewRow, date: string): Promise<void> {
    row.date = parseCsvDate(date, 'YYYY-MM-DD') ?? undefined;
    this.rowResolver.updateParseError(row, this.rowContext);
    this.resetRowConversion(row);
    await this.refreshConversions();
    await this.refreshDuplicates();
  }

  async rowValueChanged(row: ImportPreviewRow): Promise<void> {
    const rawValue = row.value as number | string | null | undefined;
    const value = Number(rawValue);
    row.value = rawValue != null && rawValue !== '' && Number.isFinite(value) ? value : undefined;
    this.rowResolver.updateParseError(row, this.rowContext);
    this.resetRowConversion(row);
    await this.refreshConversions();
    await this.refreshDuplicates();
  }

  async rowInstallmentChanged(row: ImportPreviewRow, value: string): Promise<void> {
    row.installment = parseInstallment(value) ?? undefined;
    row.createPreviousInstallments = false;
    row.createFollowingInstallments = false;
    await this.refreshDuplicates();
  }

  installmentValue(row: ImportPreviewRow): string {
    return formatImportInstallment(row.installment);
  }

  rowTagsChanged(row: ImportPreviewRow, value: string): void {
    row.tags = parseImportTags(value);
  }

  rowTagsValue(row: ImportPreviewRow): string {
    return formatImportTags(row.tags);
  }

  get hasFixedAmountMapping(): boolean {
    return this.isFixedMapping('value') || (this.separateCreditDebit && (this.isFixedMapping('credit') || this.isFixedMapping('debit')));
  }

  get hasFixedMetadataMapping(): boolean {
    return (
      this.isFixedMapping('transactionId') ||
      this.isFixedMapping('tags') ||
      this.isFixedMapping('observations') ||
      this.isFixedMapping('confirmed')
    );
  }

  async rowGroupChanged(row: ImportPreviewRow): Promise<void> {
    await this.rowResolver.groupChanged(row, this.rowContext);
  }

  categoriesFor(row: ImportPreviewRow): CategoryDto[] {
    return this.catalogs.categoriesFor(row.groupId);
  }

  categoryForRow(row: ImportPreviewRow): CategoryDto | undefined {
    return this.categoriesFor(row).find(category => category.id === row.categoryId);
  }

  rowCategoryChanged(row: ImportPreviewRow, category: CategoryDto | null | undefined): void {
    row.categoryId = category?.id;
  }

  originFor(row: ImportPreviewRow): WalletItemSearchResponseDto | undefined {
    return this.catalogs.originFor(row);
  }

  displayCurrencyFor(row: ImportPreviewRow): string {
    return this.originFor(row)?.currency ?? this.defaultCurrency;
  }

  hasValidBeneficiaries(row: ImportPreviewRow): boolean {
    return hasValidImportBeneficiaries(row.groupId, row.beneficiaries);
  }

  async openBeneficiaries(row: ImportPreviewRow): Promise<void> {
    await this.beneficiaryEditor.open(row);
  }

  addBeneficiaryLeg(): void {
    this.beneficiaryEditor.addLeg();
  }

  removeBeneficiaryLeg(index: number): void {
    this.beneficiaryEditor.removeLeg(index);
  }

  async loadBeneficiaryMembers(_page = 0, query?: string): Promise<UserForBeneficiary[]> {
    return this.beneficiaryEditor.loadMembers(_page, query);
  }

  saveBeneficiaries(): void {
    this.beneficiaryEditor.save();
  }

  async submit(): Promise<ImportBatchDto | undefined> {
    this.submitting = true;
    try {
      const result = await this.submission.submit(
        {
          canShowPreview: this.canShowPreview,
          file: this.file,
          fileHash: this.fileHash,
          rows: this.selectedRows,
        },
        key => this.importText(key),
      );
      if (result.errorKey != null) {
        this.error = this.importText(result.errorKey);
        return undefined;
      }
      this.removeFile();
      return result.batch;
    } finally {
      this.submitting = false;
    }
  }

  clearHashCheckFor(fileHash: string): void {
    if (fileHash === this.fileHash) this.hashCheck = null;
  }

  private async refreshDuplicates(): Promise<void> {
    await this.duplicates.refresh(this.rows, this.autoIgnoreDuplicates, key => this.importText(key));
  }

  private async refreshConversions(): Promise<void> {
    await this.conversions.refresh(this.rows, this.conversionContext);
  }

  private resetRowConversion(row: ImportPreviewRow): void {
    this.conversions.reset(row);
  }

  billMonth(row: ImportPreviewRow): string {
    return row.billDate?.slice(0, 7) ?? '';
  }

  setBillMonth(row: ImportPreviewRow, month: string): void {
    row.billDate = month === '' ? undefined : `${month}-01`;
  }

  private importText(key: string, params?: Record<string, unknown>): string {
    return this.translateService.instant(`financesPage.transactionsPage.importPage.${key}`, params);
  }

  private value(row: Record<string, string>, field: CsvColumnField): string | undefined {
    return this.rowResolver.value(row, field, this.rowContext);
  }

  private initializeFixedValue(field: CsvColumnField): void {
    if (this.fixedValues[field] != null) return;
    if (field === 'bill') {
      this.fixedValues.bill = dayjs().format('YYYY-MM');
    } else if (field === 'confirmed') {
      this.fixedValues.confirmed = true;
    } else {
      this.fixedValues[field] = '';
    }
  }
}

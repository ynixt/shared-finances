import { Injectable, inject } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';

import dayjs from 'dayjs';

import { ImportBatchDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/imports';
import { WalletItemSearchResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { UserForBeneficiary } from '../shared/transaction-form/transaction-form.types';
import { CsvImportBeneficiaryEditor } from './csv-import-beneficiary.editor';
import { CsvImportConversionContext } from './csv-import-conversion.service';
import {
  CSV_IMPORT_BILL_FROM_DATE_MAPPING_VALUE,
  CSV_IMPORT_FIXED_MAPPING_VALUE,
  CSV_IMPORT_MAPPING_OPTIONS,
  CSV_IMPORT_PREVIEW_PAGE_SIZE_OPTIONS,
} from './csv-import-draft.config';
import { CsvImportRowContext } from './csv-import-row.resolver';
import { CsvImportSubmissionService } from './csv-import-submission.service';
import { CsvColumnField, parseCsvDate } from './csv-statement-parser';
import { ImportDraftFileStore } from './import-draft-file.store';
import { ParsedImportSourceStatement } from './import-file-source';
import { FixedValue, ImportPreviewRow, MappingOption } from './import-transactions.models';
import { formatImportTags, hasValidImportBeneficiaries, parseImportTags } from './import-transactions.utils';

@Injectable()
export class ImportDraftStore extends ImportDraftFileStore {
  private readonly beneficiaryEditor = inject(CsvImportBeneficiaryEditor);
  private readonly submission = inject(CsvImportSubmissionService);
  private readonly translateService = inject(TranslateService);

  readonly mappingOptions: MappingOption[] = CSV_IMPORT_MAPPING_OPTIONS;
  readonly billFromDateMappingValue = CSV_IMPORT_BILL_FROM_DATE_MAPPING_VALUE;
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

  protected get rowContext(): CsvImportRowContext {
    return {
      dateFormat: this.csvStore.dateFormat,
      detectedDateFormat: this.csvStore.detectedDateFormat,
      decimalSeparator: this.csvStore.decimalSeparator,
      separateCreditDebit: this.csvStore.separateCreditDebit,
      invertValues: this.csvStore.invertValues,
      mapping: this.mapping,
      billFromDateMappingValue: this.billFromDateMappingValue,
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

  async setMapping(field: CsvColumnField, column: string): Promise<void> {
    const previousFixedCategory = this.fixedCategory;
    if (field === 'bill' && column === '') {
      this.mapping[field] = this.billFromDateMappingValue;
    } else if (field === 'origin' && column === '') {
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
      (field !== 'value' || !this.csvStore.separateCreditDebit) &&
      (field !== 'credit' || this.csvStore.separateCreditDebit) &&
      (field !== 'debit' || this.csvStore.separateCreditDebit)
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
    if (this.fileFormat === 'OFX') {
      return this.ofxStore.statements
        .filter(statement => this.rows.some(row => row.sourceStatementKey === statement.key && row.included))
        .every(statement => this.walletItems.some(item => item.id === this.ofxStore.statementOrigins[statement.key]));
    }
    return !this.isFixedMapping('origin') || this.fixedOrigin != null;
  }

  ofxStatementOrigin(statement: ParsedImportSourceStatement): WalletItemSearchResponseDto | undefined {
    return this.ofxStore.originFor(statement, this.walletItems);
  }

  async setOfxStatementOrigin(
    statement: ParsedImportSourceStatement,
    origin: WalletItemSearchResponseDto | null | undefined,
  ): Promise<void> {
    const originId = this.ofxStore.setStatementOrigin(statement, origin, this.walletItems);
    this.rows
      .filter(row => row.sourceStatementKey === statement.key)
      .forEach(row => {
        row.walletItemId = originId;
        this.rowResolver.applyBillSuggestion(row, this.rowContext);
        this.resetRowConversion(row);
      });
    await this.refreshConversions();
    await this.refreshDuplicates();
  }

  async setFixedValue(field: CsvColumnField, value: FixedValue | null | undefined): Promise<void> {
    const normalizedValue = value == null ? '' : value;
    if (this.fixedValues[field] === normalizedValue) return;

    if (field === 'group') {
      const previousCategory = this.fixedCategory;
      this.fixedValues[field] = normalizedValue;
      const groupId = String(normalizedValue);
      if (groupId !== '') {
        await this.catalogs.ensureGroupMembers(groupId);
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
    this.rowResolver.applyBillSuggestion(row, this.rowContext);
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

  async rowExternalTransactionIdChanged(): Promise<void> {
    await this.refreshDuplicates();
  }

  async rowInstallmentEnabledChanged(row: ImportPreviewRow, enabled: boolean): Promise<void> {
    row.installment = enabled ? (row.installment ?? { current: 1, total: 2 }) : undefined;
    if (!enabled) {
      row.createPreviousInstallments = false;
      row.createFollowingInstallments = false;
    }
    await this.refreshDuplicates();
  }

  async rowInstallmentPartChanged(row: ImportPreviewRow, part: 'current' | 'total', value: number | null | undefined): Promise<void> {
    if (row.installment == null) return;
    const normalized = Math.max(1, Math.trunc(value ?? 1));
    row.installment =
      part === 'current'
        ? { current: normalized, total: Math.max(normalized, row.installment.total) }
        : { current: Math.min(row.installment.current, normalized), total: normalized };
    await this.refreshDuplicates();
  }

  rowTagsChanged(row: ImportPreviewRow, value: string): void {
    row.tags = parseImportTags(value);
  }

  rowTagsValue(row: ImportPreviewRow): string {
    return formatImportTags(row.tags);
  }

  get hasFixedAmountMapping(): boolean {
    return (
      this.isFixedMapping('value') || (this.csvStore.separateCreditDebit && (this.isFixedMapping('credit') || this.isFixedMapping('debit')))
    );
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
          format: this.fileFormat ?? 'CSV',
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

  protected async refreshDuplicates(): Promise<void> {
    await this.duplicates.refresh(this.rows, this.autoIgnoreDuplicates, key => this.importText(key));
  }

  protected async refreshConversions(): Promise<void> {
    await this.conversions.refresh(this.rows, this.conversionContext);
  }

  private resetRowConversion(row: ImportPreviewRow): void {
    this.conversions.reset(row);
  }

  protected importText(key: string, params?: Record<string, unknown>): string {
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

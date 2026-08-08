import { Injectable, inject } from '@angular/core';

import dayjs from 'dayjs';

import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { CreditCardBillService } from '../../services/credit-card-bill.service';
import { UserForBeneficiary } from '../shared/transaction-form/transaction-form.types';
import { CsvImportCatalogStore } from './csv-import-catalog.store';
import {
  CsvColumnField,
  CsvColumnMapping,
  CsvDateFormat,
  normalizeHeader,
  parseBeneficiaries,
  parseCsvDate,
  parseCsvNumber,
  parseInstallment,
} from './csv-statement-parser';
import { ParsedImportSourceRow } from './import-file-source';
import { FixedValue, ImportPreviewRow } from './import-transactions.models';
import { hasValidImportBeneficiaries, parseImportBill } from './import-transactions.utils';

export interface CsvImportRowContext {
  dateFormat: CsvDateFormat;
  detectedDateFormat: Exclude<CsvDateFormat, 'AUTO'>;
  decimalSeparator: '.' | ',';
  separateCreditDebit: boolean;
  invertValues: boolean;
  mapping: CsvColumnMapping;
  billFromDateMappingValue: string;
  fixedMappingValue: string;
  fixedValues: Partial<Record<CsvColumnField, FixedValue>>;
  fixedCategory?: CategoryDto;
  text(key: string, params?: Record<string, unknown>): string;
}

@Injectable()
export class CsvImportRowResolver {
  private readonly catalogs = inject(CsvImportCatalogStore);
  private readonly creditCardBillService = inject(CreditCardBillService);

  create(raw: Record<string, string>, index: number, context: CsvImportRowContext): ImportPreviewRow {
    const format = context.dateFormat === 'AUTO' ? context.detectedDateFormat : context.dateFormat;
    const date = this.value(raw, 'date', context);
    const parsedDate =
      date == null ? null : this.isFixedMapping('date', context) ? parseCsvDate(date, 'YYYY-MM-DD') : parseCsvDate(date, format);
    let amount: number | null = null;
    if (context.separateCreditDebit) {
      const credit = this.mappedNumber(raw, 'credit', context);
      const debit = this.mappedNumber(raw, 'debit', context);
      amount = credit != null ? Math.abs(credit) : debit != null ? -Math.abs(debit) : null;
    } else {
      amount = this.mappedNumber(raw, 'value', context);
    }

    if (amount != null && context.invertValues) amount *= -1;

    const confirmed = this.isFixedMapping('confirmed', context)
      ? context.fixedValues.confirmed !== false
      : !['nao', 'não', 'false', '0'].includes((this.value(raw, 'confirmed', context) ?? 'sim').trim().toLowerCase());
    const installment = parseInstallment(this.value(raw, 'installment', context));
    const parseError =
      parsedDate == null ? context.text('validation.invalidDate') : amount == null ? context.text('validation.invalidValue') : undefined;
    const walletItemId = this.catalogs.resolveWalletItemId(this.value(raw, 'origin', context));
    const walletItem = this.catalogs.walletItems.find(item => item.id === walletItemId);
    const targetCurrency = walletItem?.currency ?? this.catalogs.defaultCurrency;
    const rawCurrency = this.value(raw, 'currency', context)?.trim();
    const currencyFromFile = this.catalogs.findKnownCurrency(rawCurrency);
    const hasInvalidFileCurrency = rawCurrency != null && rawCurrency !== '' && currencyFromFile == null;
    const currency = hasInvalidFileCurrency ? undefined : (currencyFromFile ?? targetCurrency);
    const sameCurrency = currency === targetCurrency;
    return {
      raw,
      index,
      included: parseError == null,
      duplicate: false,
      date: parsedDate ?? undefined,
      name: this.value(raw, 'description', context)?.trim() || undefined,
      value: amount ?? undefined,
      currency,
      currencySource: rawCurrency == null || rawCurrency === '' ? 'FALLBACK' : 'FILE',
      conversionRate: sameCurrency ? 1 : undefined,
      convertedValue: sameCurrency ? (amount ?? undefined) : undefined,
      convertedValueOverridden: false,
      conversionLoading: false,
      conversionTargetCurrency: targetCurrency,
      conversionError: hasInvalidFileCurrency ? context.text('conversion.unknownCurrency') : undefined,
      installment: installment ?? undefined,
      createPreviousInstallments: false,
      createFollowingInstallments: false,
      confirmed,
      beneficiaries: [],
      tags: this.value(raw, 'tags', context)
        ?.split(',')
        .map(tag => tag.trim())
        .filter(Boolean),
      observations: this.value(raw, 'observations', context)?.trim() || undefined,
      externalTransactionId: this.value(raw, 'transactionId', context)?.trim() || undefined,
      billDate: parseImportBill(this.value(raw, 'bill', context)),
      parseError,
      walletItemId,
    };
  }

  createFromSource(
    source: ParsedImportSourceRow,
    index: number,
    walletItemId: string | undefined,
    context: CsvImportRowContext,
  ): ImportPreviewRow {
    const walletItem = this.catalogs.walletItems.find(item => item.id === walletItemId);
    const targetCurrency = walletItem?.currency ?? this.catalogs.defaultCurrency;
    const rawCurrency = source.currency?.trim();
    const currencyFromFile = this.catalogs.findKnownCurrency(rawCurrency);
    const hasInvalidFileCurrency = rawCurrency != null && rawCurrency !== '' && currencyFromFile == null;
    const currency = hasInvalidFileCurrency ? undefined : (currencyFromFile ?? targetCurrency);
    const sameCurrency = currency === targetCurrency;
    const parseError =
      source.date == null
        ? context.text('validation.invalidDate')
        : source.value == null
          ? context.text('validation.invalidValue')
          : undefined;

    return {
      raw: source.raw,
      index,
      included: parseError == null,
      duplicate: false,
      date: source.date,
      name: source.name,
      value: source.value,
      currency,
      currencySource: rawCurrency == null || rawCurrency === '' ? 'FALLBACK' : 'FILE',
      conversionRate: sameCurrency ? 1 : undefined,
      convertedValue: sameCurrency ? source.value : undefined,
      convertedValueOverridden: false,
      conversionLoading: false,
      conversionTargetCurrency: targetCurrency,
      conversionError: hasInvalidFileCurrency ? context.text('conversion.unknownCurrency') : undefined,
      createPreviousInstallments: false,
      createFollowingInstallments: false,
      confirmed: true,
      beneficiaries: [],
      observations: source.observations,
      externalTransactionId: source.externalTransactionId,
      sourceStatementKey: source.sourceStatementKey,
      parseError,
      walletItemId,
    };
  }

  async resolve(rows: ImportPreviewRow[], context: CsvImportRowContext): Promise<void> {
    const groupsByName = new Map(this.catalogs.groups.map(group => [normalizeHeader(group.name), group] as const));
    for (const row of rows) {
      const group = this.isFixedMapping('group', context)
        ? this.catalogs.groups.find(candidate => candidate.id === context.fixedValues.group)
        : groupsByName.get(normalizeHeader(this.value(row.raw, 'group', context) ?? ''));
      row.groupId = group?.id;
      if (group != null) {
        await Promise.all([this.catalogs.ensureGroupCategories(group.id), this.catalogs.ensureGroupMembers(group.id)]);
      }

      if (this.isFixedMapping('category', context) && context.fixedCategory != null) {
        const available = this.catalogs.categoriesFor(row.groupId);
        row.categoryId =
          available.find(category => category.id === context.fixedCategory?.id)?.id ??
          available.find(category => category.conceptId === context.fixedCategory?.conceptId)?.id ??
          this.catalogs.findCategoryByName(available, context.fixedCategory.name)?.id;
      } else {
        row.categoryId = this.catalogs.findCategoryByName(
          this.catalogs.categoriesFor(row.groupId),
          this.value(row.raw, 'category', context),
        )?.id;
      }

      if (row.groupId != null) {
        const members = await this.catalogs.ensureGroupMembers(row.groupId);
        const membersByEmail = new Map(members.map(member => [member.email.toLowerCase(), member] as const));
        row.beneficiaries = parseBeneficiaries(this.value(row.raw, 'beneficiaries', context))
          .map(leg => ({ member: membersByEmail.get(leg.email), benefitPercent: leg.benefitPercent, email: leg.email }))
          .filter((leg): leg is { member: UserForBeneficiary; benefitPercent: number; email: string } => leg.member != null)
          .map(leg => ({ userId: leg.member.id, benefitPercent: leg.benefitPercent, email: leg.email }));
        this.ensureDefaultBeneficiary(row);
      }
    }
  }

  async groupChanged(row: ImportPreviewRow, context: CsvImportRowContext): Promise<void> {
    const previousCategory = this.catalogs.findCategoryById(row.categoryId);
    row.beneficiaries = [];
    if (row.groupId != null && row.groupId !== '') {
      this.ensureDefaultBeneficiary(row);
      const [, members] = await Promise.all([
        this.catalogs.ensureGroupCategories(row.groupId),
        this.catalogs.ensureGroupMembers(row.groupId),
      ]);
      this.ensureDefaultBeneficiary(row, members);
    }
    row.categoryId = this.catalogs.findMatchingCategory(row, previousCategory, this.value(row.raw, 'category', context))?.id;
  }

  applyBillSuggestions(rows: ImportPreviewRow[], context: CsvImportRowContext): void {
    rows.forEach(row => this.applyBillSuggestion(row, context));
  }

  applyBillSuggestion(row: ImportPreviewRow, context: CsvImportRowContext): void {
    const card = this.catalogs.originFor(row);
    if (card == null || card.type !== 'CREDIT_CARD') {
      row.billDate = undefined;
      return;
    }
    if (this.isFixedMapping('bill', context)) {
      row.billDate = parseImportBill(this.value(row.raw, 'bill', context));
      return;
    }
    if ((context.mapping.bill === context.billFromDateMappingValue || row.billDate == null) && row.date != null) {
      row.billDate = this.creditCardBillService
        .getBestBill(dayjs(row.date).toDate(), card.dueDay ?? 1, card.dueOnNextBusinessDay ?? false, card.daysBetweenDueAndClosing ?? 0)
        .format('YYYY-MM-01');
    }
  }

  updateParseError(row: ImportPreviewRow, context: CsvImportRowContext): void {
    row.parseError =
      row.date == null ? context.text('validation.invalidDate') : row.value == null ? context.text('validation.invalidValue') : undefined;
  }

  ensureDefaultBeneficiary(row: ImportPreviewRow, members: readonly UserForBeneficiary[] = []): void {
    const currentUser = this.catalogs.currentUser;
    if (row.groupId == null || row.groupId === '' || currentUser == null) return;
    const currentMember =
      members.find(member => member.id === currentUser.id || member.email.toLowerCase() === currentUser.email.toLowerCase()) ?? currentUser;
    if (hasValidImportBeneficiaries(row.groupId, row.beneficiaries)) {
      const currentBeneficiary = row.beneficiaries.find(
        beneficiary =>
          beneficiary.userId === currentUser.id ||
          beneficiary.userId === currentMember.id ||
          beneficiary.email?.toLowerCase() === currentUser.email.toLowerCase(),
      );
      if (currentBeneficiary != null) {
        currentBeneficiary.userId = currentMember.id;
        currentBeneficiary.email = currentMember.email;
      }
      return;
    }
    row.beneficiaries = [{ userId: currentMember.id, email: currentMember.email, benefitPercent: 100 }];
  }

  value(row: Record<string, string>, field: CsvColumnField, context: CsvImportRowContext): string | undefined {
    if (this.isFixedMapping(field, context)) {
      if (field === 'category') return context.fixedCategory?.name;
      if (field === 'group') {
        const groupId = String(context.fixedValues.group ?? '');
        return this.catalogs.groups.find(group => group.id === groupId)?.name;
      }
      const fixedValue = context.fixedValues[field];
      if (fixedValue == null || fixedValue === '') return undefined;
      if (typeof fixedValue === 'boolean') return fixedValue ? 'sim' : 'não';
      return String(fixedValue);
    }
    const header = context.mapping[field];
    return header == null ? undefined : row[header];
  }

  private mappedNumber(row: Record<string, string>, field: 'credit' | 'debit' | 'value', context: CsvImportRowContext): number | null {
    if (this.isFixedMapping(field, context)) {
      const rawFixedValue = context.fixedValues[field];
      if (rawFixedValue == null || rawFixedValue === '') return null;
      const fixedValue = Number(rawFixedValue);
      return Number.isFinite(fixedValue) ? fixedValue : null;
    }
    return parseCsvNumber(this.value(row, field, context) ?? '', context.decimalSeparator);
  }

  private isFixedMapping(field: CsvColumnField, context: CsvImportRowContext): boolean {
    return context.mapping[field] === context.fixedMappingValue;
  }
}

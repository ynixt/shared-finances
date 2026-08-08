import { describe, expect, it } from 'vitest';

import {
  convertImportValue,
  formatImportInstallment,
  formatImportTags,
  hasValidImportBeneficiaries,
  parseImportBill,
  parseImportTags,
} from './import-transactions.utils';

describe('import transaction utilities', () => {
  it('parses ISO and localized bill months and rejects invalid months', () => {
    expect(parseImportBill('2026-08')).toBe('2026-08-01');
    expect(parseImportBill('8/2026')).toBe('2026-08-01');
    expect(parseImportBill('13/2026')).toBeUndefined();
  });

  it('parses and formats tags without empty values', () => {
    expect(parseImportTags(' mercado, , casa ')).toEqual(['mercado', 'casa']);
    expect(formatImportTags(['mercado', 'casa'])).toBe('mercado, casa');
  });

  it('converts monetary values with two decimal places', () => {
    expect(convertImportValue(10, 5.123)).toBe(51.23);
    expect(convertImportValue(undefined, 5)).toBeUndefined();
  });

  it('formats installments', () => {
    expect(formatImportInstallment({ current: 2, total: 6 })).toBe('2/6');
    expect(formatImportInstallment(undefined)).toBe('');
  });

  it('validates beneficiary uniqueness and percentage totals only for grouped rows', () => {
    expect(hasValidImportBeneficiaries(undefined, [])).toBe(true);
    expect(hasValidImportBeneficiaries('group', [])).toBe(false);
    expect(
      hasValidImportBeneficiaries('group', [
        { userId: 'a', benefitPercent: 50 },
        { userId: 'b', benefitPercent: 50 },
      ]),
    ).toBe(true);
    expect(
      hasValidImportBeneficiaries('group', [
        { userId: 'a', benefitPercent: 50 },
        { userId: 'a', benefitPercent: 50 },
      ]),
    ).toBe(false);
  });
});

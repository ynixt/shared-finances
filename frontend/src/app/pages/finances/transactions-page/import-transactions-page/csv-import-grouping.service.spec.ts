import { TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { CsvImportCatalogStore } from './csv-import-catalog.store';
import { CsvImportGroupingService } from './csv-import-grouping.service';
import { ImportPreviewRow } from './import-transactions.models';

describe('CsvImportGroupingService', () => {
  let service: CsvImportGroupingService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CsvImportGroupingService,
        {
          provide: CsvImportCatalogStore,
          useValue: { originFor: (row: ImportPreviewRow) => ({ name: row.walletItemId === 'origin' ? 'Checking' : 'Savings' }) },
        },
      ],
    });
    service = TestBed.inject(CsvImportGroupingService);
  });

  it('collapses a valid opposite-sign pair into the negative preview row', () => {
    const debit = row(0, -100, 'origin', 'transfer-1');
    const credit = row(1, 95, 'target', 'transfer-1');

    service.apply([debit, credit], key => key);

    expect(debit.transferDisplayName).toBe('Checking → Savings');
    expect(debit.previewHidden).toBe(false);
    expect(credit.previewHidden).toBe(true);
    expect(credit.transferPreviewLeaderIndex).toBe(debit.index);
  });

  it('marks same-sign and oversized transfer groups invalid', () => {
    const rows = [row(0, -10, 'origin', 'bad'), row(1, -20, 'target', 'bad'), row(2, 30, 'target', 'bad')];

    service.apply(rows, key => key);

    expect(rows.every(candidate => !candidate.included)).toBe(true);
    expect(rows.every(candidate => candidate.parseError === 'validation.invalidTransferGroup')).toBe(true);
  });

  function row(index: number, value: number, walletItemId: string, transferGroupId: string): ImportPreviewRow {
    return {
      raw: {},
      index,
      included: true,
      duplicate: false,
      date: '2026-08-11',
      value,
      currency: 'BRL',
      currencySource: 'FILE',
      convertedValue: value,
      convertedValueOverridden: false,
      conversionLoading: false,
      conversionTargetCurrency: 'BRL',
      createPreviousInstallments: false,
      createFollowingInstallments: false,
      confirmed: true,
      beneficiaries: [],
      walletItemId,
      transferGroupId,
    };
  }
});

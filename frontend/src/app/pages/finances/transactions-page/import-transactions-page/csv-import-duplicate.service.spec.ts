import { TestBed } from '@angular/core/testing';

import { MessageService } from 'primeng/api';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ImportService } from '../../services/import.service';
import { CsvImportDuplicateService } from './csv-import-duplicate.service';
import { ImportPreviewRow } from './import-transactions.models';

describe('CsvImportDuplicateService', () => {
  const imports = { checkDuplicates: vi.fn() };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        CsvImportDuplicateService,
        { provide: ImportService, useValue: imports },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });
  });

  it('maps duplicate indexes against checkable rows and applies auto-ignore', async () => {
    imports.checkDuplicates.mockResolvedValue([1]);
    const rows = [row(undefined), row('wallet-1'), row('wallet-2')];

    await TestBed.inject(CsvImportDuplicateService).refresh(rows, true, key => key);

    expect(rows[1].duplicate).toBe(false);
    expect(rows[2]).toEqual(expect.objectContaining({ duplicate: true, included: false }));
  });
});

function row(walletItemId?: string): ImportPreviewRow {
  return {
    raw: {},
    index: 0,
    included: true,
    duplicate: false,
    date: '2026-08-08',
    value: 10,
    convertedValue: 10,
    currencySource: 'FALLBACK',
    convertedValueOverridden: false,
    conversionLoading: false,
    conversionTargetCurrency: 'BRL',
    createPreviousInstallments: false,
    createFollowingInstallments: false,
    confirmed: true,
    beneficiaries: [],
    walletItemId,
  };
}

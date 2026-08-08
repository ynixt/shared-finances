import { TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ExchangeRateService } from '../../services/exchange-rate.service';
import { CsvImportConversionService } from './csv-import-conversion.service';
import { ImportPreviewRow } from './import-transactions.models';

describe('CsvImportConversionService', () => {
  const exchangeRates = { resolve: vi.fn() };
  const context = { displayCurrency: () => 'BRL', text: (key: string) => key };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [CsvImportConversionService, { provide: ExchangeRateService, useValue: exchangeRates }],
    });
  });

  it('groups equal quotes and applies the resolved rate to every row', async () => {
    exchangeRates.resolve.mockResolvedValue([{ fromCurrency: 'USD', toCurrency: 'BRL', referenceDate: '2026-08-08', rate: 5 }]);
    const rows = [row(10), row(20)];

    await TestBed.inject(CsvImportConversionService).refresh(rows, context);

    expect(exchangeRates.resolve).toHaveBeenCalledWith([{ fromCurrency: 'USD', toCurrency: 'BRL', referenceDate: '2026-08-08' }]);
    expect(rows.map(item => item.convertedValue)).toEqual([50, 100]);
  });

  it('discards an obsolete response after a newer refresh', async () => {
    let resolveFirst!: (value: unknown[]) => void;
    exchangeRates.resolve
      .mockReturnValueOnce(new Promise(resolve => (resolveFirst = resolve)))
      .mockResolvedValueOnce([{ fromCurrency: 'USD', toCurrency: 'BRL', referenceDate: '2026-08-08', rate: 6 }]);
    const service = TestBed.inject(CsvImportConversionService);
    const first = row(10);
    const second = row(10);

    const pending = service.refresh([first], context);
    await service.refresh([second], context);
    resolveFirst([{ fromCurrency: 'USD', toCurrency: 'BRL', referenceDate: '2026-08-08', rate: 4 }]);
    await pending;

    expect(second.convertedValue).toBe(60);
    expect(first.convertedValue).toBeUndefined();
  });
});

function row(value: number): ImportPreviewRow {
  return {
    raw: {},
    index: value,
    included: true,
    duplicate: false,
    date: '2026-08-08',
    value,
    currency: 'USD',
    currencySource: 'FILE',
    convertedValueOverridden: false,
    conversionLoading: false,
    conversionTargetCurrency: 'BRL',
    createPreviousInstallments: false,
    createFollowingInstallments: false,
    confirmed: true,
    beneficiaries: [],
  };
}

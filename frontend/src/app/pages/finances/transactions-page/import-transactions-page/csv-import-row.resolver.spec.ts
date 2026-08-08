import { TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CreditCardBillService } from '../../services/credit-card-bill.service';
import { CsvImportCatalogStore } from './csv-import-catalog.store';
import { CsvImportRowContext, CsvImportRowResolver } from './csv-import-row.resolver';

describe('CsvImportRowResolver', () => {
  const currentUser = {
    id: 'user',
    email: 'user@example.com',
    firstName: 'User',
    lastName: 'Test',
    label: 'User Test (user@example.com)',
  };
  const category = { id: 'food', name: 'Mercado', conceptId: 'food' };
  const catalogs = {
    defaultCurrency: 'BRL',
    walletItems: [{ id: 'wallet', currency: 'BRL' }],
    groups: [{ id: 'group', name: 'Casa' }],
    currentUser,
    resolveWalletItemId: vi.fn((value?: string) => (value === 'wallet' ? 'wallet' : undefined)),
    findKnownCurrency: vi.fn((value?: string) => (value === 'Real brasileiro' ? 'BRL' : undefined)),
    categoriesFor: vi.fn(() => [category]),
    findCategoryByName: vi.fn((_categories: unknown, name?: string) => (name?.toLowerCase() === 'mercado' ? category : undefined)),
    ensureGroupCategories: vi.fn(() => Promise.resolve([category])),
    ensureGroupMembers: vi.fn(() => Promise.resolve([currentUser])),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        CsvImportRowResolver,
        { provide: CsvImportCatalogStore, useValue: catalogs },
        { provide: CreditCardBillService, useValue: { getBestBill: vi.fn() } },
      ],
    });
  });

  it('creates a preview row using the mapped fields and catalog currency name', () => {
    const resolver = TestBed.inject(CsvImportRowResolver);
    const row = resolver.create(
      { data: '08/08/2026', valor: '10,50', origem: 'wallet', moeda: 'Real brasileiro' },
      0,
      context({ date: 'data', value: 'valor', origin: 'origem', currency: 'moeda' }),
    );

    expect(row).toEqual(expect.objectContaining({ date: '2026-08-08', value: 10.5, walletItemId: 'wallet', currency: 'BRL' }));
  });

  it('derives revenue and expense only from the signed value and ignores a legacy type column', () => {
    const resolver = TestBed.inject(CsvImportRowResolver);
    const mapping = { date: 'data', value: 'valor', type: 'tipo' } as const;

    const expense = resolver.create({ data: '08/08/2026', valor: '-10', tipo: 'receita' }, 0, context(mapping));
    const revenue = resolver.create({ data: '08/08/2026', valor: '10', tipo: 'despesa' }, 1, context(mapping));

    expect(expense.value).toBe(-10);
    expect(revenue.value).toBe(10);
  });

  it('resolves group, contextual category and default beneficiary', async () => {
    const resolver = TestBed.inject(CsvImportRowResolver);
    const row = resolver.create(
      { data: '08/08/2026', valor: '10', grupo: 'Casa', categoria: 'Mercado' },
      0,
      context({ date: 'data', value: 'valor', group: 'grupo', category: 'categoria' }),
    );

    await resolver.resolve([row], context({ date: 'data', value: 'valor', group: 'grupo', category: 'categoria' }));

    expect(row.groupId).toBe('group');
    expect(row.categoryId).toBe('food');
    expect(row.beneficiaries).toEqual([{ userId: 'user', email: 'user@example.com', benefitPercent: 100 }]);
  });
});

function context(mapping: CsvImportRowContext['mapping']): CsvImportRowContext {
  return {
    dateFormat: 'DD/MM/YYYY',
    detectedDateFormat: 'DD/MM/YYYY',
    decimalSeparator: ',',
    separateCreditDebit: false,
    invertValues: false,
    mapping,
    billFromDateMappingValue: '__BILL_FROM_DATE__',
    fixedMappingValue: '__FIXED_VALUE__',
    fixedValues: {},
    text: key => key,
  };
}

import { TestBed } from '@angular/core/testing';

import { of } from 'rxjs';

import fs from 'node:fs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CurrencyCatalogService } from '../../../../components/currency-selector/currency-catalog.service';
import { UserService } from '../../../../services/user.service';
import { CreditCardBillService } from '../../services/credit-card-bill.service';
import { GroupService } from '../../services/group.service';
import { ImportService } from '../../services/import.service';
import { WalletItemService } from '../../services/wallet-item.service';
import { CsvImportCatalogStore } from './csv-import-catalog.store';
import { CsvImportRowContext, CsvImportRowResolver } from './csv-import-row.resolver';
import { parseCsv } from './csv-statement-parser';
import { detectCsvLayout } from './layout-providers/csv-layout-providers';

describe('Kotlin export to TypeScript import round trip', () => {
  const members = [
    { id: 'alice', email: 'alice@example.com', firstName: 'Alice', lastName: 'A', label: 'Alice A (alice@example.com)' },
    { id: 'bob', email: 'bob@example.com', firstName: 'Bob', lastName: 'B', label: 'Bob B (bob@example.com)' },
  ];
  const destinationWallets = [
    { id: 'checking', name: 'Checking', currency: 'BRL', type: 'BANK_ACCOUNT', showOnDashboard: true },
    { id: 'savings', name: 'Savings', currency: 'BRL', type: 'BANK_ACCOUNT', showOnDashboard: true },
    { id: 'card', name: 'Card', currency: 'BRL', type: 'CREDIT_CARD', showOnDashboard: true },
  ];
  const category = { id: 'food', name: 'Food', conceptId: 'food-concept', color: '#fff' };
  const walletItems = { getAllItems: vi.fn() };
  const importService = { categoryCatalog: vi.fn() };
  const groupService = { getAllGroups: vi.fn(), findAllMembers: vi.fn() };

  beforeEach(async () => {
    vi.clearAllMocks();
    walletItems.getAllItems.mockResolvedValue({ content: destinationWallets, last: true });
    importService.categoryCatalog.mockResolvedValue({
      personal: [],
      groups: [{ groupId: 'household', categories: [category], members }],
    });
    groupService.getAllGroups.mockResolvedValue([{ id: 'household', name: 'Household' }]);
    groupService.findAllMembers.mockResolvedValue(members.map(user => ({ user })));
    TestBed.configureTestingModule({
      providers: [
        CsvImportCatalogStore,
        CsvImportRowResolver,
        { provide: CreditCardBillService, useValue: { getBestBill: vi.fn() } },
        { provide: WalletItemService, useValue: walletItems },
        { provide: ImportService, useValue: importService },
        { provide: GroupService, useValue: groupService },
        {
          provide: UserService,
          useValue: { getUser: () => Promise.resolve({ ...members[0], defaultCurrency: 'BRL' }) },
        },
        { provide: CurrencyCatalogService, useValue: { getCurrencies: () => of([{ code: 'BRL', name: 'Brazilian real' }]) } },
      ],
    });
    await TestBed.inject(CsvImportCatalogStore).load();
  });

  it('preserves the canonical fields through the real parser and row resolver', async () => {
    const csv = fs.readFileSync(
      'src/app/pages/finances/transactions-page/import-transactions-page/fixtures/transaction-export-round-trip.csv',
      'utf8',
    );
    const parsed = parseCsv(csv, { delimiter: ';', decimalSeparator: '.', dateFormat: 'AUTO' });
    const detected = detectCsvLayout(parsed.headers);

    expect(parsed.layoutProviderId).toBe('shared-finances-csv-template-v1');
    expect(detected.providerId).toBe('shared-finances-csv-template-v1');
    expect(Object.keys(detected.mapping)).toHaveLength(20);

    const context: CsvImportRowContext = {
      dateFormat: 'AUTO',
      detectedDateFormat: parsed.detectedDateFormat,
      decimalSeparator: '.',
      separateCreditDebit: false,
      invertValues: false,
      mapping: parsed.mapping,
      billFromDateMappingValue: '__BILL_FROM_DATE__',
      fixedMappingValue: '__FIXED_VALUE__',
      fixedValues: {},
      text: key => key,
    };
    const resolver = TestBed.inject(CsvImportRowResolver);
    const catalogs = TestBed.inject(CsvImportCatalogStore);
    const indexSpy = vi.spyOn(catalogs, 'createReferenceIndex');
    const transportCallsBeforeResolution =
      walletItems.getAllItems.mock.calls.length +
      importService.categoryCatalog.mock.calls.length +
      groupService.findAllMembers.mock.calls.length;
    const rows = parsed.rows.map((raw, index) => resolver.create(raw, index, context));
    await resolver.resolve(rows, context);

    expect(indexSpy).toHaveBeenCalledTimes(1);
    expect(
      walletItems.getAllItems.mock.calls.length +
        importService.categoryCatalog.mock.calls.length +
        groupService.findAllMembers.mock.calls.length,
    ).toBe(transportCallsBeforeResolution);
    expect(rows.every(row => row.walletItemId != null && row.categoryId === 'food' && row.groupId === 'household')).toBe(true);

    const simple = rows.find(row => row.externalTransactionId === 'simple-transaction')!;
    expect(simple.tags).toEqual(['home', 'food']);
    expect(simple.beneficiaries).toEqual([
      { userId: 'alice', email: 'alice@example.com', benefitPercent: 60 },
      { userId: 'bob', email: 'bob@example.com', benefitPercent: 40 },
    ]);
    expect(simple.confirmed).toBe(true);
    expect(simple.name).toBe('market; "weekly"\nrun');
    expect(simple.observations).toBe('first line\nsecond; "quoted" line');

    const transfer = rows.filter(row => row.transferGroupId === 'transfer-1');
    expect(transfer).toHaveLength(2);
    expect(transfer.map(row => row.value)).toEqual([-100, 100]);
    expect(transfer.every(row => row.confirmed === false)).toBe(true);

    const series = rows.filter(row => row.seriesGroupId === 'series-1');
    expect(series).toHaveLength(3);
    expect(series.map(row => row.installment)).toEqual([
      { current: 1, total: 12 },
      { current: 2, total: 12 },
      { current: 3, total: 12 },
    ]);
  });
});

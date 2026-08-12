import { TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { ImportDraftStore } from './import-draft.store';
import {
  creditCardBillService,
  csvStore,
  exchangeRateService,
  importService,
  ofxStore,
  previewRow,
  setupImportDraftStore,
  walletItem,
} from './import-draft.store.spec-harness';
import { ParsedImportSourceStatement } from './import-file-source';
import { ImportPreviewRow } from './import-transactions.models';

describe('ImportDraftStore CSV preview', () => {
  beforeEach(setupImportDraftStore);
  it('keeps CSV parsing state outside the common draft store', () => {
    const sourceStore = csvStore();
    sourceStore.delimiter = ',';
    sourceStore.load(new TextEncoder().encode('date,description,value\n2026-08-08,Coffee,12.50').buffer as ArrayBuffer);

    const parsed = sourceStore.parse();

    expect(parsed.headers).toEqual(['date', 'description', 'value']);
    expect(parsed.rows).toHaveLength(1);
    expect(TestBed.inject(ImportDraftStore)).not.toHaveProperty('fileText');
  });

  it('keeps OFX statements and account mappings outside the common draft store', () => {
    const sourceStore = ofxStore();
    const statement: ParsedImportSourceStatement = {
      accountId: '123456789',
      key: 'BANK:001:123456789:0',
      kind: 'BANK',
      maskedAccountId: '•••• 6789',
      pendingCount: 0,
      rows: [],
    };
    const wallet = {
      id: 'wallet-1',
      currency: 'BRL',
      name: 'Conta',
      showOnDashboard: true,
      type: 'BANK_ACCOUNT' as const,
    };
    sourceStore.statements = [statement];

    sourceStore.setStatementOrigin(statement, wallet, [wallet]);

    expect(sourceStore.originFor(statement, [wallet])).toBe(wallet);
    expect(TestBed.inject(ImportDraftStore)).not.toHaveProperty('ofxStatements');
  });

  it('paginates the preview with 20 items by default and accepts all configured page sizes', () => {
    const component = TestBed.inject(ImportDraftStore);
    component.rows = Array.from({ length: 120 }, (_, index) => previewRow(index));

    expect(component.previewPageSizeOptions).toEqual([20, 50, 100, 500]);
    expect(component.pagedRows).toHaveLength(20);
    expect(component.pagedRows[0].index).toBe(0);

    component.previewPageChanged({ first: 50, rows: 50 });
    expect(component.pagedRows).toHaveLength(50);
    expect(component.pagedRows[0].index).toBe(50);

    component.previewPageChanged({ first: 0, rows: 100 });
    expect(component.pagedRows).toHaveLength(100);
  });

  it('does not expose transaction type as a column mapping option', () => {
    const component = TestBed.inject(ImportDraftStore);

    expect(component.mappingOptions.some(option => option.field === 'type')).toBe(false);
  });

  it('uses the bill derived from the row date by default and recomputes it after a date edit', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.currencyOptions = ['BRL'];
    component.defaultCurrency = 'BRL';
    component.walletItems = [walletItem('wallet-card', 'BRL', 'CREDIT_CARD')];
    csvStore().fileText = [
      'origin;origin_name;date;description;value;currency;category;category_name;category_concept_id;group;group_name;installment;beneficiaries;bill;tags;observations;confirmed;transaction id;transfer id;series id',
      ['wallet-card', 'Cartao', '2026-08-07', 'Compra', '10', 'BRL', '', '', '', '', '', '', '', '', '', '', '', '', '', ''].join(';'),
    ].join('\n');

    await component.reprocess(true);

    expect(component.mapping.origin).toBe('origin');
    expect(component.mapping.bill).toBe(component.billFromDateMappingValue);
    expect(component.fixedMappingOptions.some(option => option.field === 'origin')).toBe(false);
    expect(component.fixedMappingOptions.some(option => option.field === 'bill')).toBe(false);
    expect(component.originFor(component.rows[0])?.type).toBe('CREDIT_CARD');
    expect(creditCardBillService.getBestBill).toHaveBeenCalled();
    expect(component.rows[0]).toMatchObject({ walletItemId: 'wallet-card', billDate: '2026-08-01' });

    await component.rowDateChanged(component.rows[0], '2026-09-07');

    expect(component.rows[0].billDate).toBe('2026-09-01');
  });

  it.each(['CSV', 'OFX'] as const)('edits installment parts and converts a %s row back to a single purchase', async fileFormat => {
    const component = TestBed.inject(ImportDraftStore);
    const row: ImportPreviewRow = previewRow(0);
    component.fileFormat = fileFormat;
    component.rows = [row];

    await component.rowInstallmentEnabledChanged(row, true);
    expect(row.installment).toEqual({ current: 1, total: 2 });

    await component.rowInstallmentPartChanged(row, 'current', 4);
    expect(row.installment).toEqual({ current: 4, total: 4 });

    await component.rowInstallmentPartChanged(row, 'total', 3);
    expect(row.installment).toEqual({ current: 3, total: 3 });

    row.createPreviousInstallments = true;
    row.createFollowingInstallments = true;
    await component.rowInstallmentEnabledChanged(row, false);

    expect(row.installment).toBeUndefined();
    expect(row.createPreviousInstallments).toBe(false);
    expect(row.createFollowingInstallments).toBe(false);
  });

  it('submits a CSV installment detected from the file as a single purchase after the user disables installments', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.currencyOptions = ['BRL'];
    component.defaultCurrency = 'BRL';
    component.walletItems = [walletItem('wallet-card', 'BRL', 'CREDIT_CARD')];
    component.fixedValues.origin = 'wallet-card';
    component.file = new File(['csv'], 'installment.csv', { type: 'text/csv' });
    component.fileFormat = 'CSV';
    component.fileHash = 'hash-installment';
    csvStore().fileText = [
      'origin;origin_name;date;description;value;currency;category;category_name;category_concept_id;group;group_name;installment;beneficiaries;bill;tags;observations;confirmed;transaction id;transfer id;series id',
      [
        'wallet-card',
        'Cartao',
        '2026-08-07',
        'Compra parcelada',
        '10',
        'BRL',
        '',
        '',
        '',
        '',
        '',
        '3/12',
        '',
        '',
        '',
        '',
        '',
        '',
        '',
        '',
      ].join(';'),
    ].join('\n');

    await component.reprocess(true);
    expect(component.rows[0].installment).toEqual({ current: 3, total: 12 });

    await component.rowInstallmentEnabledChanged(component.rows[0], false);
    await component.submit();

    expect(importService.create.mock.calls[0][0].lines[0]).toMatchObject({
      installment: undefined,
      installmentTotal: undefined,
      createPreviousInstallments: false,
      createFollowingInstallments: false,
    });
  });

  it('applies a fixed value to every row and reapplies it over manual preview edits', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.currencyOptions = ['BRL'];
    component.defaultCurrency = 'BRL';
    csvStore().fileText = 'data;descricao;valor\n07/08/2026;Compra A;10\n08/08/2026;Compra B;20\n';
    await component.reprocess(true);
    await component.setMapping('description', component.fixedMappingValue);

    await component.setFixedValue('description', 'Descrição fixa');

    expect(component.rows.map(row => row.name)).toEqual(['Descrição fixa', 'Descrição fixa']);

    component.rows[0].name = 'Alteração manual';
    component.rows[1].included = false;
    await component.setFixedValue('description', 'Nova descrição fixa');

    expect(component.rows.map(row => row.name)).toEqual(['Nova descrição fixa', 'Nova descrição fixa']);
    expect(component.rows[1].included).toBe(false);
  });

  it('hides the preview and only enables confirmation after a fixed origin is selected', () => {
    const component = TestBed.inject(ImportDraftStore);
    component.file = new File(['csv'], 'statement.csv', { type: 'text/csv' });
    component.walletItems = [walletItem('wallet-1')];
    component.mapping.origin = component.fixedMappingValue;
    component.rows = [{ ...previewRow(0), walletItemId: undefined }];

    expect(component.canShowPreview).toBe(false);
    expect(component.canSubmit).toBe(false);

    component.fixedValues.origin = 'wallet-1';
    component.rows[0].walletItemId = 'wallet-1';
    expect(component.canShowPreview).toBe(true);
    expect(component.canSubmit).toBe(true);
  });

  it('checks duplicates only once when the fixed origin changes', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.currencyOptions = ['BRL'];
    component.defaultCurrency = 'BRL';
    component.walletItems = [walletItem('wallet-1')];
    csvStore().fileText = 'data;descricao;valor\n07/08/2026;Compra;10\n';
    await component.reprocess(true);
    importService.checkDuplicates.mockClear();

    await component.setFixedValue('origin', 'wallet-1');

    expect(importService.checkDuplicates).toHaveBeenCalledOnce();
  });

  it('uses the user default currency for impact before an origin is selected', () => {
    const component = TestBed.inject(ImportDraftStore);
    component.defaultCurrency = 'BRL';
    component.rows = [{ ...previewRow(0), convertedValue: 52.5 }];

    expect(component.displayCurrencyFor(component.rows[0])).toBe('BRL');
    expect(component.balanceImpacts).toEqual([{ currency: 'BRL', value: 52.5 }]);
  });

  it('resolves a detected foreign currency into the effective currency', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.defaultCurrency = 'BRL';
    component.currencyOptions = ['BRL', 'USD'];
    component.walletItems = [walletItem('wallet-brl')];
    component.rows = [
      {
        ...previewRow(0),
        walletItemId: 'wallet-brl',
        currency: 'USD',
        currencySource: 'FILE',
        convertedValue: undefined,
        conversionRate: undefined,
      },
    ];
    exchangeRateService.resolve.mockResolvedValue([
      {
        fromCurrency: 'USD',
        toCurrency: 'BRL',
        referenceDate: '2026-08-07',
        quoteDate: '2026-08-07',
        rate: 5.25,
      },
    ]);

    await component.rowOriginChanged(component.rows[0]);

    expect(exchangeRateService.resolve).toHaveBeenCalledWith([{ fromCurrency: 'USD', toCurrency: 'BRL', referenceDate: '2026-08-07' }]);
    expect(component.rows[0].conversionRate).toBe(5.25);
    expect(component.rows[0].convertedValue).toBe(52.5);
    expect(component.balanceImpacts).toEqual([{ currency: 'BRL', value: 52.5 }]);
  });

  it('moves fallback currencies to the selected origin currency without conversion', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.defaultCurrency = 'BRL';
    component.walletItems = [walletItem('wallet-eur', 'EUR')];
    component.rows = [{ ...previewRow(0), walletItemId: 'wallet-eur', currency: 'BRL', conversionTargetCurrency: 'BRL' }];

    await component.rowOriginChanged(component.rows[0]);

    expect(component.rows[0].currency).toBe('EUR');
    expect(component.rows[0].conversionRate).toBe(1);
    expect(component.rows[0].convertedValue).toBe(10);
    expect(exchangeRateService.resolve).not.toHaveBeenCalled();
  });

  it('keeps a manually overridden converted value', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.defaultCurrency = 'BRL';
    component.rows = [
      {
        ...previewRow(0),
        currency: 'USD',
        currencySource: 'FILE',
        convertedValue: 50,
        conversionRate: 5,
      },
    ];
    component.rows[0].convertedValue = 49.9;

    await component.convertedValueChanged(component.rows[0]);

    expect(component.rows[0].convertedValueOverridden).toBe(true);
    expect(component.rows[0].convertedValue).toBe(49.9);
    expect(component.balanceImpacts).toEqual([{ currency: 'BRL', value: 49.9 }]);
  });

  it('leaves an imported currency blank when it is not available in the selector catalog', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.defaultCurrency = 'BRL';
    component.currencyOptions = ['BRL', 'USD'];
    csvStore().fileText = 'data;descricao;valor;moeda\n2026-08-07;Compra;10;XYZ\n';

    await component.reprocess(true);
    await component.setMapping('currency', 'moeda');

    expect(component.rows[0].currency).toBeUndefined();
    expect(component.rows[0].convertedValue).toBeUndefined();
    expect(component.rows[0].conversionError).toBe('Moeda não reconhecida no catálogo');
  });
});

import { TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { CsvImportCatalogStore } from './csv-import-catalog.store';
import { ImportDraftStore } from './import-draft.store';
import {
  csvStore,
  groupService,
  importService,
  ofxStore,
  previewRow,
  setupImportDraftStore,
  walletItem,
} from './import-draft.store.spec-harness';

describe('ImportDraftStore OFX and categories', () => {
  beforeEach(setupImportDraftStore);
  it('loads an OFX draft, maps its account, and submits only normalized data', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.importPreferencesLoaded = true;
    component.currencyOptions = ['BRL'];
    component.defaultCurrency = 'BRL';
    component.walletItems = [walletItem('wallet-ofx')];
    const ofx = `<?xml version="1.0" encoding="UTF-8"?><OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><CURDEF>BRL</CURDEF><BANKACCTFROM><BANKID>001</BANKID><ACCTID>123456789</ACCTID></BANKACCTFROM><BANKTRANLIST><STMTTRN><DTPOSTED>20260807</DTPOSTED><TRNAMT>-42.50</TRNAMT><FITID>ofx-001</FITID><NAME>Mercado</NAME></STMTTRN></BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>`;
    const file = new File([ofx], 'statement.ofx', { type: 'application/x-ofx' });
    Object.defineProperty(file, 'arrayBuffer', { value: () => Promise.resolve(new TextEncoder().encode(ofx).buffer) });

    await component.selectFile({ target: { files: [file] } } as unknown as Event);

    expect(component.fileFormat).toBe('OFX');
    expect(ofxStore().statements).toHaveLength(1);
    expect(component.rows[0]).toMatchObject({
      name: 'Mercado',
      value: -42.5,
      externalTransactionId: 'ofx-001',
      walletItemId: undefined,
    });
    expect(component.canShowPreview).toBe(false);

    await component.setOfxStatementOrigin(ofxStore().statements[0], component.walletItems[0]);
    expect(component.canShowPreview).toBe(true);

    await component.rowInstallmentEnabledChanged(component.rows[0], true);
    await component.rowInstallmentPartChanged(component.rows[0], 'total', 6);
    await component.rowInstallmentPartChanged(component.rows[0], 'current', 2);
    component.rows[0].createPreviousInstallments = true;
    component.rows[0].createFollowingInstallments = true;

    await component.submit();

    const request = importService.create.mock.calls[0][0];
    expect(request).toMatchObject({
      fileName: 'statement.ofx',
      format: 'OFX',
      lines: [
        expect.objectContaining({
          walletItemId: 'wallet-ofx',
          externalTransactionId: 'ofx-001',
          installment: 2,
          installmentTotal: 6,
          createPreviousInstallments: true,
          createFollowingInstallments: true,
        }),
      ],
    });
    expect(JSON.stringify(request)).not.toContain('123456789');
    expect(JSON.stringify(request)).not.toContain('<OFX>');
    expect(ofxStore().statements).toEqual([]);
    expect(component.file).toBeUndefined();
  });

  it('applies one fixed bill month to every OFX row and preserves it when the card changes', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.importPreferencesLoaded = true;
    component.currencyOptions = ['BRL'];
    component.defaultCurrency = 'BRL';
    component.walletItems = [walletItem('wallet-card-a', 'BRL', 'CREDIT_CARD'), walletItem('wallet-card-b', 'BRL', 'CREDIT_CARD')];
    const ofx = `<?xml version="1.0" encoding="UTF-8"?><OFX><CREDITCARDMSGSRSV1><CCSTMTTRNRS><CCSTMTRS><CURDEF>BRL</CURDEF><CCACCTFROM><ACCTID>123456789</ACCTID></CCACCTFROM><BANKTRANLIST><STMTTRN><DTPOSTED>20260807</DTPOSTED><TRNAMT>-42.50</TRNAMT><FITID>ofx-card-001</FITID><NAME>Compra A</NAME></STMTTRN><STMTTRN><DTPOSTED>20260808</DTPOSTED><TRNAMT>-18.90</TRNAMT><FITID>ofx-card-002</FITID><NAME>Compra B</NAME></STMTTRN></BANKTRANLIST></CCSTMTRS></CCSTMTTRNRS></CREDITCARDMSGSRSV1></OFX>`;
    const file = new File([ofx], 'card.ofx', { type: 'application/x-ofx' });
    Object.defineProperty(file, 'arrayBuffer', { value: () => Promise.resolve(new TextEncoder().encode(ofx).buffer) });

    await component.selectFile({ target: { files: [file] } } as unknown as Event);

    expect(component.mapping.bill).toBe(component.billFromDateMappingValue);
    expect(component.fixedMappingOptions.some(option => option.field === 'bill')).toBe(false);

    await component.setOfxStatementOrigin(ofxStore().statements[0], component.walletItems[0]);
    expect(component.rows.map(row => row.billDate)).toEqual(['2026-08-01', '2026-08-01']);

    await component.rowDateChanged(component.rows[0], '2026-09-07');
    await component.setOfxStatementOrigin(ofxStore().statements[0], component.walletItems[1]);
    expect(component.rows.map(row => row.billDate)).toEqual(['2026-09-01', '2026-08-01']);

    await component.setMapping('bill', component.fixedMappingValue);
    await component.setFixedValue('bill', '2026-09');

    expect(component.rows.map(row => row.billDate)).toEqual(['2026-09-01', '2026-09-01']);

    await component.setOfxStatementOrigin(ofxStore().statements[0], component.walletItems[0]);

    expect(component.rows.map(row => row.walletItemId)).toEqual(['wallet-card-a', 'wallet-card-a']);
    expect(component.rows.map(row => row.billDate)).toEqual(['2026-09-01', '2026-09-01']);
  });

  it('requires every OFX statement with selected rows to be mapped independently', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.currencyOptions = ['BRL', 'USD'];
    component.defaultCurrency = 'BRL';
    component.walletItems = [walletItem('wallet-bank'), walletItem('wallet-card', 'USD', 'CREDIT_CARD')];
    component.fileFormat = 'OFX';
    ofxStore().statements = [
      {
        accountId: '11112222',
        currency: 'BRL',
        key: 'bank',
        kind: 'BANK',
        maskedAccountId: '•••• 2222',
        pendingCount: 1,
        rows: [
          {
            currency: 'BRL',
            date: '2026-08-01',
            externalTransactionId: 'bank-1',
            name: 'Bank row',
            raw: {},
            sourceStatementKey: 'bank',
            value: 10,
          },
        ],
      },
      {
        accountId: '99998888',
        currency: 'USD',
        key: 'card',
        kind: 'CREDIT_CARD',
        maskedAccountId: '•••• 8888',
        pendingCount: 0,
        rows: [
          {
            currency: 'USD',
            date: '2026-08-02',
            externalTransactionId: 'card-1',
            name: 'Card row',
            raw: {},
            sourceStatementKey: 'card',
            value: -5,
          },
        ],
      },
    ];
    component.mapping = { confirmed: component.fixedMappingValue };
    component.fixedValues = { confirmed: true };

    await component.reprocess(true);
    await component.setOfxStatementOrigin(ofxStore().statements[0], component.walletItems[0]);
    expect(component.canShowPreview).toBe(false);

    await component.setOfxStatementOrigin(ofxStore().statements[1], component.walletItems[1]);
    expect(component.canShowPreview).toBe(true);
    expect(component.rows.map(row => row.walletItemId)).toEqual(['wallet-bank', 'wallet-card']);
  });

  it('prefills the exact Supermercado category from the C6 Categoria column', async () => {
    const component = TestBed.inject(ImportDraftStore);
    const category = {
      id: '019fdaff-21c8-7418-9502-0f26c2670772',
      name: 'Supermercado',
      color: '#27AE60',
      conceptId: '045ec597-062a-4a3e-92f2-fffd2a1a4f3b',
      children: null,
      parentId: null,
    };
    component.categories = [category];
    component.currencyOptions = ['BRL'];
    component.defaultCurrency = 'BRL';
    csvStore().fileText =
      'Data de Compra;Nome no Cartão;Final do Cartão;Categoria;Descrição;Parcela;Valor (em US$);Cotação (em R$);Valor (em R$)\n' +
      '04/07/2026;GABRIEL A SILVA;9668;Supermercado;CACAU SHOW;Única;0;0;5.29\n';

    await component.reprocess(true);

    expect(component.mapping.category).toBe('Categoria');
    expect(component.rows[0].categoryId).toBe(category.id);
    expect(component.categoryForRow(component.rows[0])).toBe(category);
  });

  it('maps a personal category to the corresponding group category when the group changes', async () => {
    const component = TestBed.inject(ImportDraftStore);
    const catalogs = TestBed.inject(CsvImportCatalogStore);
    const personalCategory = {
      id: 'category-personal',
      name: 'Mercado pessoal',
      color: '#ffffff',
      conceptId: 'concept-market',
      children: [],
    };
    const groupCategory = {
      id: 'category-group',
      name: 'Mercado do grupo',
      color: '#ffffff',
      conceptId: 'concept-market',
      children: [],
    };
    component.categories = [personalCategory];
    (
      catalogs as unknown as {
        categoriesByGroup: Map<string, typeof component.categories>;
      }
    ).categoriesByGroup.set('group-1', [groupCategory]);
    groupService.findAllMembers.mockResolvedValue([]);
    const row = {
      ...previewRow(0),
      categoryId: personalCategory.id,
      groupId: 'group-1',
      raw: { categoria: 'Mercado pessoal' },
    };

    await component.rowGroupChanged(row);

    expect(row.categoryId).toBe(groupCategory.id);
    expect(component.categoriesFor(row)).toEqual([groupCategory]);
    expect(importService.categoryCatalog).not.toHaveBeenCalled();
  });

  it('clears the category when the selected group has no corresponding category', async () => {
    const component = TestBed.inject(ImportDraftStore);
    const catalogs = TestBed.inject(CsvImportCatalogStore);
    const personalCategory = {
      id: 'category-personal',
      name: 'Mercado',
      color: '#ffffff',
      conceptId: 'concept-market',
      children: [],
    };
    component.categories = [personalCategory];
    (
      catalogs as unknown as {
        categoriesByGroup: Map<string, typeof component.categories>;
      }
    ).categoriesByGroup.set('group-1', []);
    groupService.findAllMembers.mockResolvedValue([]);
    const row = {
      ...previewRow(0),
      categoryId: personalCategory.id,
      groupId: 'group-1',
    };

    await component.rowGroupChanged(row);

    expect(row.categoryId).toBeUndefined();
  });
});

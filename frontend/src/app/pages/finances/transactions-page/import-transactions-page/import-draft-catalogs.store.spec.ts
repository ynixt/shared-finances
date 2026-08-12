import { TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { CsvImportCatalogStore } from './csv-import-catalog.store';
import { ImportDraftStore } from './import-draft.store';
import { csvStore, groupService, importService, previewRow, setupImportDraftStore, walletItem } from './import-draft.store.spec-harness';

describe('ImportDraftStore catalogs and limits', () => {
  beforeEach(setupImportDraftStore);
  it('resolves an origin UUID from a mapped CSV column and keeps it editable per row', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.defaultCurrency = 'BRL';
    component.currencyOptions = ['BRL', 'USD'];
    component.walletItems = [walletItem('019fdb00-a88b-775d-806a-8d74982081ea'), walletItem('wallet-usd', 'USD')];
    csvStore().fileText = 'origem;data;descricao;valor\n019fdb00-a88b-775d-806a-8d74982081ea;2026-08-07;Compra;10\n';
    await component.reprocess(true);

    expect(component.mapping.origin).toBe(component.fixedMappingValue);
    expect(component.canShowPreview).toBe(false);

    await component.setMapping('origin', 'origem');
    expect(component.canShowPreview).toBe(true);
    expect(component.rows[0].walletItemId).toBe('019fdb00-a88b-775d-806a-8d74982081ea');

    component.rows[0].walletItemId = 'wallet-usd';
    await component.rowOriginChanged(component.rows[0]);
    expect(component.originFor(component.rows[0])?.name).toBe('Conta wallet-usd');
    expect(component.rows[0].currency).toBe('USD');
  });

  it('detects the date format after the user maps a date column from an unknown layout', async () => {
    const component = TestBed.inject(ImportDraftStore);
    csvStore().fileText = 'Quando;Detalhes\n2026-08-07;Compra\n';

    await component.reprocess(true);

    expect(csvStore().detectedLayoutProviderId).toBeUndefined();
    expect(component.mapping.date).toBeUndefined();

    await component.setMapping('date', 'Quando');

    expect(csvStore().detectedDateFormat).toBe('YYYY-MM-DD');
    expect(component.rows[0].date).toBe('2026-08-07');
  });

  it('groups preview impacts by each row origin currency', () => {
    const component = TestBed.inject(ImportDraftStore);
    component.walletItems = [walletItem('wallet-brl'), walletItem('wallet-usd', 'USD')];
    component.rows = [
      { ...previewRow(0), walletItemId: 'wallet-brl', convertedValue: 25 },
      { ...previewRow(1), walletItemId: 'wallet-usd', convertedValue: 10 },
    ];

    expect(component.balanceImpacts).toEqual([
      { currency: 'BRL', value: 25 },
      { currency: 'USD', value: 10 },
    ]);
  });

  it('sends the resolved origin inside each imported line', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.file = new File(['csv'], 'statement.csv', { type: 'text/csv' });
    component.fileHash = 'hash';
    component.mapping.origin = 'origem';
    component.rows = [
      { ...previewRow(0), walletItemId: 'wallet-1', value: -10, convertedValue: -10 },
      { ...previewRow(1), walletItemId: 'wallet-2', value: -20, convertedValue: -20 },
    ];

    await component.submit();

    expect(importService.create).toHaveBeenCalledWith(
      expect.objectContaining({
        lines: [expect.objectContaining({ walletItemId: 'wallet-1' }), expect.objectContaining({ walletItemId: 'wallet-2' })],
      }),
    );
    expect(importService.create.mock.calls[0][0]).not.toHaveProperty('walletItemId');
    expect(importService.get).not.toHaveBeenCalled();
  });

  it('sets the current user as the default beneficiary when a group is selected', async () => {
    const component = TestBed.inject(ImportDraftStore);
    const catalogs = TestBed.inject(CsvImportCatalogStore);
    const currentUser = {
      id: 'user-1',
      email: 'user@example.com',
      firstName: 'Usuário',
      lastName: 'Atual',
      label: 'Usuário Atual (user@example.com)',
    };
    catalogs.currentUser = currentUser;
    groupService.findAllMembers.mockResolvedValue([{ user: currentUser }]);
    component.rows = [{ ...previewRow(0), groupId: 'group-1' }];

    await component.rowGroupChanged(component.rows[0]);

    expect(component.rows[0].beneficiaries).toEqual([{ userId: 'user-1', email: 'user@example.com', benefitPercent: 100 }]);
    expect(component.selectedRows).toHaveLength(1);
  });

  it('sets the default beneficiary before lazy group members finish loading', async () => {
    const component = TestBed.inject(ImportDraftStore);
    const catalogs = TestBed.inject(CsvImportCatalogStore);
    const currentUser = {
      id: 'user-1',
      email: 'user@example.com',
      firstName: 'Usuário',
      lastName: 'Atual',
      label: 'Usuário Atual (user@example.com)',
    };
    catalogs.currentUser = currentUser;
    let resolveMembers!: (members: Array<{ user: typeof currentUser }>) => void;
    groupService.findAllMembers.mockReturnValue(
      new Promise(resolve => {
        resolveMembers = resolve;
      }),
    );
    const row = { ...previewRow(0), groupId: 'group-1' };

    const groupChange = component.rowGroupChanged(row);

    expect(row.beneficiaries).toEqual([{ userId: 'user-1', email: 'user@example.com', benefitPercent: 100 }]);

    resolveMembers([{ user: currentUser }]);
    await groupChange;
    await component.openBeneficiaries(row);

    expect(component.beneficiaryForm.get('primaryBeneficiaryUser')?.value).toEqual(currentUser);
    expect(component.beneficiaryDialogVisible).toBe(true);
    expect(groupService.findAllMembers).toHaveBeenCalledTimes(1);
  });

  it('maps the category picker value to the import category id', () => {
    const component = TestBed.inject(ImportDraftStore);
    const category = {
      id: 'category-1',
      name: 'Mercado',
      color: '#ffffff',
      conceptId: 'concept-1',
      children: [],
    };
    const row = { ...previewRow(0), categoryId: category.id };
    component.categories = [category];

    expect(component.categoryForRow(row)).toBe(category);

    component.rowCategoryChanged(row, undefined);
    expect(row.categoryId).toBeUndefined();
  });

  it('preloads complete personal and group category catalogs before the preview', async () => {
    const component = TestBed.inject(ImportDraftStore);
    const personalCategory = {
      id: 'category-personal',
      name: 'Pessoal',
      color: '#ffffff',
      conceptId: 'concept-personal',
      children: [],
    };
    const groupCategory = {
      id: 'category-group',
      name: 'Grupo',
      color: '#ffffff',
      conceptId: 'concept-group',
      children: [],
    };
    importService.categoryCatalog.mockResolvedValue({
      personal: [personalCategory],
      groups: [{ groupId: 'group-1', categories: [groupCategory], members: [] }],
    });
    groupService.getAllGroups.mockResolvedValue([{ id: 'group-1', name: 'Casa', permissions: [], role: 'ADMIN' }]);

    await component.initialize();

    expect(component.categories).toEqual([personalCategory]);
    expect(component.categoriesFor({ ...previewRow(0), groupId: 'group-1' })).toEqual([groupCategory]);
    expect(importService.categoryCatalog).toHaveBeenCalledOnce();
  });

  it('loads the configured import line limit with the page catalogs', async () => {
    const component = TestBed.inject(ImportDraftStore);
    importService.preferences.mockResolvedValue({ maxLines: 2500 });

    await component.initialize();

    expect(component.maxLines).toBe(2500);
    expect(component.importPreferencesLoaded).toBe(true);
    expect(component.loading).toBe(false);
  });

  it('keeps imports unbounded when the user has no plan line bound', async () => {
    const component = TestBed.inject(ImportDraftStore);
    importService.preferences.mockResolvedValue({ maxLines: null });

    await component.initialize();
    component.file = new File(['csv'], 'statement.csv', { type: 'text/csv' });
    csvStore().fileText = 'data;descricao;valor\n07/08/2026;Compra A;10\n08/08/2026;Compra B;20\n09/08/2026;Compra C;30\n';
    await component.reprocess(true);

    expect(component.maxLines).toBeNull();
    expect(component.rows).toHaveLength(3);
    expect(component.error).toBeUndefined();
  });

  it('keeps file processing disabled when import preferences cannot be loaded', async () => {
    const component = TestBed.inject(ImportDraftStore);
    importService.preferences.mockRejectedValue(new Error('preferences unavailable'));

    await component.initialize();

    expect(component.maxLines).toBeNull();
    expect(component.importPreferencesLoaded).toBe(false);
    expect(component.error).toBe('financesPage.transactionsPage.importPage.errors.loadData');
    expect(component.loading).toBe(false);
  });

  it('accepts exactly the configured number of parsed data rows and ignores blank rows and the header', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.maxLines = 2;
    component.file = new File(['csv'], 'statement.csv', { type: 'text/csv' });
    csvStore().fileText = 'data;descricao;valor\n\n07/08/2026;Compra A;10\n\n08/08/2026;Compra B;20\n';

    await component.reprocess(true);

    expect(component.file).toBeDefined();
    expect(component.rows).toHaveLength(2);
    expect(component.error).toBeUndefined();
  });

  it('removes a CSV one line over the limit before duplicate checks', async () => {
    const component = TestBed.inject(ImportDraftStore);
    component.maxLines = 2;
    component.file = new File(['csv'], 'statement.csv', { type: 'text/csv' });
    csvStore().fileText = 'data;descricao;valor\n07/08/2026;Compra A;10\n08/08/2026;Compra B;20\n09/08/2026;Compra C;30\n';
    importService.checkDuplicates.mockClear();

    await component.reprocess(true);

    expect(component.file).toBeUndefined();
    expect(component.rows).toEqual([]);
    expect(component.error).toBe('financesPage.transactionsPage.importPage.errors.lineLimitExceeded');
    expect(importService.checkDuplicates).not.toHaveBeenCalled();
  });

  it('fills a mapped CSV category automatically when a personal category matches', async () => {
    const component = TestBed.inject(ImportDraftStore);
    const category = {
      id: 'category-personal',
      name: 'Mercado',
      color: '#ffffff',
      conceptId: 'concept-market',
      children: [],
    };
    component.categories = [category];
    component.currencyOptions = ['BRL'];
    component.defaultCurrency = 'BRL';
    csvStore().fileText = 'data;descricao;valor;categoria\n07/08/2026;Compra;10;mercado\n';

    await component.reprocess(true);
    await component.setMapping('category', 'categoria');

    expect(component.rows[0].categoryId).toBe(category.id);
  });
});

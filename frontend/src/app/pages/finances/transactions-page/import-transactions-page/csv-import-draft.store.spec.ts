import { provideHttpClient } from '@angular/common/http';
import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';

import { of } from 'rxjs';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CurrencyCatalogService } from '../../../../components/currency-selector/currency-catalog.service';
import { UserService } from '../../../../services/user.service';
import { CreditCardBillService } from '../../services/credit-card-bill.service';
import { ExchangeRateService } from '../../services/exchange-rate.service';
import { GroupCategoriesService } from '../../services/group-categories.service';
import { GroupService } from '../../services/group.service';
import { ImportService } from '../../services/import.service';
import { UserCategoriesService } from '../../services/user-categories.service';
import { WalletItemService } from '../../services/wallet-item.service';
import { CsvImportBeneficiaryEditor } from './csv-import-beneficiary.editor';
import { CsvImportCatalogStore } from './csv-import-catalog.store';
import { CsvImportConversionService } from './csv-import-conversion.service';
import { CsvImportDraftStore } from './csv-import-draft.store';
import { CsvImportDuplicateService } from './csv-import-duplicate.service';
import { CsvImportRowResolver } from './csv-import-row.resolver';
import { CsvImportSubmissionService } from './csv-import-submission.service';

describe('CsvImportDraftStore', () => {
  const importService = {
    checkDuplicates: vi.fn().mockResolvedValue([]),
    preferences: vi.fn().mockResolvedValue({ maxLines: 1000 }),
    create: vi.fn(),
    get: vi.fn(),
    list: vi.fn().mockResolvedValue([]),
    undo: vi.fn(),
  };
  const exchangeRateService = {
    resolve: vi.fn(),
  };
  const groupCategoriesService = {
    getAllCategories: vi.fn().mockResolvedValue({ content: [] }),
  };
  const groupService = {
    findAllMembers: vi.fn(),
    getAllGroups: vi.fn().mockResolvedValue([]),
  };
  const userCategoriesService = {
    getAllCategories: vi.fn().mockResolvedValue({ content: [] }),
  };
  const walletItemService = {
    getAllItems: vi.fn().mockResolvedValue({ content: [] }),
  };
  const currencyCatalogService = {
    getCurrencies: vi.fn().mockReturnValue(of([{ code: 'BRL', name: 'Real', symbol: 'R$' }])),
  };
  const messageService = {
    add: vi.fn(),
  };
  const translations: Record<string, string> = {
    'financesPage.transactionsPage.importPage.conversion.unknownCurrency': 'Moeda não reconhecida no catálogo',
    'financesPage.transactionsPage.importPage.notifications.importCompleted.summary': 'Importação concluída',
    'financesPage.transactionsPage.importPage.notifications.undoCompleted.summary': 'Importação desfeita',
    'financesPage.transactionsPage.importPage.notifications.undoStarted.summary': 'Desfazimento iniciado',
    'financesPage.transactionsPage.importPage.status.failed': 'Falhou',
    'financesPage.transactionsPage.importPage.status.imported': 'Importado',
    'financesPage.transactionsPage.importPage.status.inProgress': 'Em andamento',
    'financesPage.transactionsPage.importPage.status.undoFailed': 'Falha ao desfazer',
    'financesPage.transactionsPage.importPage.status.undoing': 'Desfazendo',
  };
  const translateService = {
    instant: vi.fn((key: string) => translations[key] ?? key),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    importService.checkDuplicates.mockResolvedValue([]);
    importService.preferences.mockResolvedValue({ maxLines: 1000 });
    importService.create.mockResolvedValue({
      id: 'batch-new',
      fileHash: 'hash',
      fileName: 'statement.csv',
      format: 'CSV',
      walletItemName: 'Múltiplas origens',
      qty: 2,
      totalCredit: 0,
      totalDebit: 30,
      createdAt: '2026-08-07T12:00:00Z',
      status: 'QUEUED',
      retries: 0,
    });
    importService.get.mockResolvedValue({
      id: 'batch-new',
      fileHash: 'hash',
      fileName: 'statement.csv',
      format: 'CSV',
      walletItemName: 'Múltiplas origens',
      qty: 2,
      totalCredit: 0,
      totalDebit: 30,
      createdAt: '2026-08-07T12:00:00Z',
      finishedAt: '2026-08-07T12:00:01Z',
      status: 'COMPLETED',
      retries: 1,
    });
    exchangeRateService.resolve.mockResolvedValue([]);
    groupCategoriesService.getAllCategories.mockResolvedValue({ content: [] });
    groupService.getAllGroups.mockResolvedValue([]);
    userCategoriesService.getAllCategories.mockResolvedValue({ content: [] });
    walletItemService.getAllItems.mockResolvedValue({ content: [] });
    importService.list.mockResolvedValue([]);
    currencyCatalogService.getCurrencies.mockReturnValue(of([{ code: 'BRL', name: 'Real', symbol: 'R$' }]));
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      providers: [
        CsvImportCatalogStore,
        CsvImportRowResolver,
        CsvImportConversionService,
        CsvImportDuplicateService,
        CsvImportBeneficiaryEditor,
        CsvImportSubmissionService,
        CsvImportDraftStore,
        FormBuilder,
        provideHttpClient(),
        { provide: ImportService, useValue: importService },
        { provide: ExchangeRateService, useValue: exchangeRateService },
        {
          provide: UserService,
          useValue: {
            getUser: vi.fn().mockResolvedValue({
              id: 'user-1',
              email: 'user@example.com',
              firstName: 'Usuário',
              lastName: 'Atual',
              defaultCurrency: 'BRL',
            }),
          },
        },
        { provide: WalletItemService, useValue: walletItemService },
        { provide: UserCategoriesService, useValue: userCategoriesService },
        { provide: GroupCategoriesService, useValue: groupCategoriesService },
        { provide: GroupService, useValue: groupService },
        { provide: CurrencyCatalogService, useValue: currencyCatalogService },
        { provide: CreditCardBillService, useValue: {} },
        { provide: MessageService, useValue: messageService },
        { provide: TranslateService, useValue: translateService },
      ],
    }).compileComponents();
  });

  it('paginates the preview with 20 items by default and accepts all configured page sizes', () => {
    const component = TestBed.inject(CsvImportDraftStore);
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
    const component = TestBed.inject(CsvImportDraftStore);

    expect(component.mappingOptions.some(option => option.field === 'type')).toBe(false);
  });

  it('uses a fixed bill with the current month by default', async () => {
    const component = TestBed.inject(CsvImportDraftStore);
    component.currencyOptions = ['BRL'];
    component.defaultCurrency = 'BRL';
    component.walletItems = [walletItem('wallet-card', 'BRL', 'CREDIT_CARD')];
    component.fixedValues.origin = 'wallet-card';
    component.fileText = 'data;descricao;valor\n07/08/2026;Compra;10\n';

    await component.reprocess(true);

    expect(component.mapping.origin).toBe(component.fixedMappingValue);
    expect(component.mapping.bill).toBe(component.fixedMappingValue);
    expect(component.fixedMappingOptions[0].field).toBe('origin');
    expect(component.fixedValues.bill).toBe(dayjs().format('YYYY-MM'));
    expect(component.rows[0].walletItemId).toBe('wallet-card');
    expect(component.rows[0].billDate).toBe(`${dayjs().format('YYYY-MM')}-01`);
  });

  it('applies a fixed value to every row and reapplies it over manual preview edits', async () => {
    const component = TestBed.inject(CsvImportDraftStore);
    component.currencyOptions = ['BRL'];
    component.defaultCurrency = 'BRL';
    component.fileText = 'data;descricao;valor\n07/08/2026;Compra A;10\n08/08/2026;Compra B;20\n';
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
    const component = TestBed.inject(CsvImportDraftStore);
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
    const component = TestBed.inject(CsvImportDraftStore);
    component.currencyOptions = ['BRL'];
    component.defaultCurrency = 'BRL';
    component.walletItems = [walletItem('wallet-1')];
    component.fileText = 'data;descricao;valor\n07/08/2026;Compra;10\n';
    await component.reprocess(true);
    importService.checkDuplicates.mockClear();

    await component.setFixedValue('origin', 'wallet-1');

    expect(importService.checkDuplicates).toHaveBeenCalledOnce();
  });

  it('uses the user default currency for impact before an origin is selected', () => {
    const component = TestBed.inject(CsvImportDraftStore);
    component.defaultCurrency = 'BRL';
    component.rows = [{ ...previewRow(0), convertedValue: 52.5 }];

    expect(component.displayCurrencyFor(component.rows[0])).toBe('BRL');
    expect(component.balanceImpacts).toEqual([{ currency: 'BRL', value: 52.5 }]);
  });

  it('resolves a detected foreign currency into the effective currency', async () => {
    const component = TestBed.inject(CsvImportDraftStore);
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
    const component = TestBed.inject(CsvImportDraftStore);
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
    const component = TestBed.inject(CsvImportDraftStore);
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
    const component = TestBed.inject(CsvImportDraftStore);
    component.defaultCurrency = 'BRL';
    component.currencyOptions = ['BRL', 'USD'];
    component.fileText = 'data;descricao;valor;moeda\n2026-08-07;Compra;10;XYZ\n';

    await component.reprocess(true);
    await component.setMapping('currency', 'moeda');

    expect(component.rows[0].currency).toBeUndefined();
    expect(component.rows[0].convertedValue).toBeUndefined();
    expect(component.rows[0].conversionError).toBe('Moeda não reconhecida no catálogo');
  });

  it('resolves an origin UUID from a mapped CSV column and keeps it editable per row', async () => {
    const component = TestBed.inject(CsvImportDraftStore);
    component.defaultCurrency = 'BRL';
    component.currencyOptions = ['BRL', 'USD'];
    component.walletItems = [walletItem('019fdb00-a88b-775d-806a-8d74982081ea'), walletItem('wallet-usd', 'USD')];
    component.fileText = 'origem;data;descricao;valor\n019fdb00-a88b-775d-806a-8d74982081ea;2026-08-07;Compra;10\n';
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
    const component = TestBed.inject(CsvImportDraftStore);
    component.fileText = 'Quando;Detalhes\n2026-08-07;Compra\n';

    await component.reprocess(true);

    expect(component.detectedLayoutProviderId).toBeUndefined();
    expect(component.mapping.date).toBeUndefined();

    await component.setMapping('date', 'Quando');

    expect(component.detectedDateFormat).toBe('YYYY-MM-DD');
    expect(component.rows[0].date).toBe('2026-08-07');
  });

  it('groups preview impacts by each row origin currency', () => {
    const component = TestBed.inject(CsvImportDraftStore);
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
    const component = TestBed.inject(CsvImportDraftStore);
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
    const component = TestBed.inject(CsvImportDraftStore);
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
    const component = TestBed.inject(CsvImportDraftStore);
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
    const component = TestBed.inject(CsvImportDraftStore);
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

  it('preloads personal and accessible group categories once for the page', async () => {
    const component = TestBed.inject(CsvImportDraftStore);
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
    userCategoriesService.getAllCategories.mockResolvedValue({ content: [personalCategory] });
    groupService.getAllGroups.mockResolvedValue([{ id: 'group-1', name: 'Casa', permissions: [], role: 'ADMIN' }]);
    groupCategoriesService.getAllCategories.mockResolvedValue({ content: [groupCategory] });

    await component.initialize();

    expect(component.categories).toEqual([personalCategory]);
    expect(component.categoriesFor({ ...previewRow(0), groupId: 'group-1' })).toEqual([groupCategory]);
    expect(groupCategoriesService.getAllCategories).toHaveBeenCalledTimes(1);
    expect(groupCategoriesService.getAllCategories).toHaveBeenCalledWith('group-1', {}, { size: 500, page: 0, sort: 'name' });
  });

  it('loads the configured import line limit with the page catalogs', async () => {
    const component = TestBed.inject(CsvImportDraftStore);
    importService.preferences.mockResolvedValue({ maxLines: 2500 });

    await component.initialize();

    expect(component.maxLines).toBe(2500);
    expect(component.importPreferencesLoaded).toBe(true);
    expect(component.loading).toBe(false);
  });

  it('keeps file processing disabled when import preferences cannot be loaded', async () => {
    const component = TestBed.inject(CsvImportDraftStore);
    importService.preferences.mockRejectedValue(new Error('preferences unavailable'));

    await component.initialize();

    expect(component.maxLines).toBe(1000);
    expect(component.importPreferencesLoaded).toBe(false);
    expect(component.error).toBe('financesPage.transactionsPage.importPage.errors.loadData');
    expect(component.loading).toBe(false);
  });

  it('accepts exactly the configured number of parsed data rows and ignores blank rows and the header', async () => {
    const component = TestBed.inject(CsvImportDraftStore);
    component.maxLines = 2;
    component.file = new File(['csv'], 'statement.csv', { type: 'text/csv' });
    component.fileText = 'data;descricao;valor\n\n07/08/2026;Compra A;10\n\n08/08/2026;Compra B;20\n';

    await component.reprocess(true);

    expect(component.file).toBeDefined();
    expect(component.rows).toHaveLength(2);
    expect(component.error).toBeUndefined();
  });

  it('removes a CSV one line over the limit before duplicate checks', async () => {
    const component = TestBed.inject(CsvImportDraftStore);
    component.maxLines = 2;
    component.file = new File(['csv'], 'statement.csv', { type: 'text/csv' });
    component.fileText = 'data;descricao;valor\n07/08/2026;Compra A;10\n08/08/2026;Compra B;20\n09/08/2026;Compra C;30\n';
    importService.checkDuplicates.mockClear();

    await component.reprocess(true);

    expect(component.file).toBeUndefined();
    expect(component.rows).toEqual([]);
    expect(component.error).toBe('financesPage.transactionsPage.importPage.errors.lineLimitExceeded');
    expect(importService.checkDuplicates).not.toHaveBeenCalled();
  });

  it('fills a mapped CSV category automatically when a personal category matches', async () => {
    const component = TestBed.inject(CsvImportDraftStore);
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
    component.fileText = 'data;descricao;valor;categoria\n07/08/2026;Compra;10;mercado\n';

    await component.reprocess(true);
    await component.setMapping('category', 'categoria');

    expect(component.rows[0].categoryId).toBe(category.id);
  });

  it('prefills the exact Supermercado category from the C6 Categoria column', async () => {
    const component = TestBed.inject(CsvImportDraftStore);
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
    component.fileText =
      'Data de Compra;Nome no Cartão;Final do Cartão;Categoria;Descrição;Parcela;Valor (em US$);Cotação (em R$);Valor (em R$)\n' +
      '04/07/2026;GABRIEL A SILVA;9668;Supermercado;CACAU SHOW;Única;0;0;5.29\n';

    await component.reprocess(true);

    expect(component.mapping.category).toBe('Categoria');
    expect(component.rows[0].categoryId).toBe(category.id);
    expect(component.categoryForRow(component.rows[0])).toBe(category);
  });

  it('maps a personal category to the corresponding group category when the group changes', async () => {
    const component = TestBed.inject(CsvImportDraftStore);
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
    expect(groupCategoriesService.getAllCategories).not.toHaveBeenCalled();
  });

  it('clears the category when the selected group has no corresponding category', async () => {
    const component = TestBed.inject(CsvImportDraftStore);
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

  function previewRow(index: number) {
    return {
      beneficiaries: [],
      confirmed: true,
      createFollowingInstallments: false,
      createPreviousInstallments: false,
      convertedValue: 10,
      convertedValueOverridden: false,
      conversionLoading: false,
      conversionTargetCurrency: 'USD',
      currency: 'USD',
      currencySource: 'FALLBACK' as const,
      date: '2026-08-07',
      duplicate: false,
      included: true,
      index,
      name: `Transaction ${index}`,
      raw: {},
      value: 10,
      walletItemId: 'wallet-1',
    };
  }

  function walletItem(id: string, currency = 'BRL', type: 'BANK_ACCOUNT' | 'CREDIT_CARD' = 'BANK_ACCOUNT') {
    return {
      id,
      currency,
      name: `Conta ${id}`,
      showOnDashboard: true,
      type,
    };
  }
});

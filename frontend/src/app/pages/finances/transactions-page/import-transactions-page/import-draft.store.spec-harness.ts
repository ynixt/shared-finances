import { provideHttpClient } from '@angular/common/http';
import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';

import { of } from 'rxjs';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { vi } from 'vitest';

import { CurrencyCatalogService } from '../../../../components/currency-selector/currency-catalog.service';
import { UserService } from '../../../../services/user.service';
import { CreditCardBillService } from '../../services/credit-card-bill.service';
import { ExchangeRateService } from '../../services/exchange-rate.service';
import { GroupService } from '../../services/group.service';
import { ImportService } from '../../services/import.service';
import { WalletItemService } from '../../services/wallet-item.service';
import { CsvImportBeneficiaryEditor } from './csv-import-beneficiary.editor';
import { CsvImportCatalogStore } from './csv-import-catalog.store';
import { CsvImportConversionService } from './csv-import-conversion.service';
import { CsvImportDraftStore } from './csv-import-draft.store';
import { CsvImportDuplicateService } from './csv-import-duplicate.service';
import { CsvImportGroupingService } from './csv-import-grouping.service';
import { CsvImportRowResolver } from './csv-import-row.resolver';
import { CsvImportSubmissionService } from './csv-import-submission.service';
import { ImportDraftStore } from './import-draft.store';
import { OfxImportDraftStore } from './ofx-import-draft.store';

export const csvStore = () => TestBed.inject(CsvImportDraftStore);
export const ofxStore = () => TestBed.inject(OfxImportDraftStore);
export const importService = {
  categoryCatalog: vi.fn().mockResolvedValue({ personal: [], groups: [] }),
  checkHash: vi.fn().mockResolvedValue({ status: 'NOT_IMPORTED' }),
  checkDuplicates: vi.fn().mockResolvedValue([]),
  preferences: vi.fn().mockResolvedValue({ maxLines: 1000 }),
  create: vi.fn(),
  get: vi.fn(),
  list: vi.fn().mockResolvedValue([]),
  undo: vi.fn(),
};
export const exchangeRateService = { resolve: vi.fn() };
export const groupService = {
  findAllMembers: vi.fn(),
  getAllGroups: vi.fn().mockResolvedValue([]),
};
export const walletItemService = {
  getAllItems: vi.fn().mockResolvedValue({ content: [] }),
};
export const currencyCatalogService = {
  getCurrencies: vi.fn().mockReturnValue(of([{ code: 'BRL', name: 'Real', symbol: 'R$' }])),
};
export const creditCardBillService = {
  getBestBill: vi.fn((date: Date) => dayjs(date).startOf('month')),
};
export const messageService = { add: vi.fn() };

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

export async function setupImportDraftStore(): Promise<void> {
  vi.clearAllMocks();
  importService.checkHash.mockResolvedValue({ status: 'NOT_IMPORTED' });
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
  importService.categoryCatalog.mockResolvedValue({ personal: [], groups: [] });
  groupService.getAllGroups.mockResolvedValue([]);
  walletItemService.getAllItems.mockResolvedValue({ content: [] });
  importService.list.mockResolvedValue([]);
  currencyCatalogService.getCurrencies.mockReturnValue(of([{ code: 'BRL', name: 'Real', symbol: 'R$' }]));
  creditCardBillService.getBestBill.mockImplementation((date: Date) => dayjs(date).startOf('month'));
  TestBed.resetTestingModule();
  await TestBed.configureTestingModule({
    providers: [
      CsvImportCatalogStore,
      CsvImportRowResolver,
      CsvImportConversionService,
      CsvImportDuplicateService,
      CsvImportGroupingService,
      CsvImportBeneficiaryEditor,
      CsvImportSubmissionService,
      CsvImportDraftStore,
      OfxImportDraftStore,
      ImportDraftStore,
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
      { provide: GroupService, useValue: groupService },
      { provide: CurrencyCatalogService, useValue: currencyCatalogService },
      { provide: CreditCardBillService, useValue: creditCardBillService },
      { provide: MessageService, useValue: messageService },
      { provide: TranslateService, useValue: translateService },
    ],
  }).compileComponents();
}

export function previewRow(index: number) {
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

export function walletItem(id: string, currency = 'BRL', type: 'BANK_ACCOUNT' | 'CREDIT_CARD' = 'BANK_ACCOUNT') {
  return {
    id,
    currency,
    name: `Conta ${id}`,
    showOnDashboard: true,
    type,
  };
}

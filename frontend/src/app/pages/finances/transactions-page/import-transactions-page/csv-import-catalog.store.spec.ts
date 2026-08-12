import { TestBed } from '@angular/core/testing';

import { of } from 'rxjs';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CurrencyCatalogService } from '../../../../components/currency-selector/currency-catalog.service';
import { UserService } from '../../../../services/user.service';
import { GroupService } from '../../services/group.service';
import { ImportService } from '../../services/import.service';
import { WalletItemService } from '../../services/wallet-item.service';
import { CsvImportCatalogStore } from './csv-import-catalog.store';

describe('CsvImportCatalogStore', () => {
  const groupService = { getAllGroups: vi.fn(), findAllMembers: vi.fn() };
  const importService = { categoryCatalog: vi.fn() };
  const walletItems = { getAllItems: vi.fn() };

  beforeEach(() => {
    vi.clearAllMocks();
    importService.categoryCatalog.mockResolvedValue({
      personal: [{ id: 'personal-food', name: 'Alimentação', conceptId: 'food' }],
      groups: [{ groupId: 'group', categories: [{ id: 'group-food', name: 'Alimentação', conceptId: 'food' }], members: [] }],
    });
    groupService.getAllGroups.mockResolvedValue([{ id: 'group', name: 'Casa' }]);
    groupService.findAllMembers.mockResolvedValue([]);
    walletItems.getAllItems.mockResolvedValue({ content: [{ id: 'wallet' }] });
    TestBed.configureTestingModule({
      providers: [
        CsvImportCatalogStore,
        { provide: CurrencyCatalogService, useValue: { getCurrencies: () => of([{ code: 'BRL', name: 'Real brasileiro' }]) } },
        {
          provide: UserService,
          useValue: {
            getUser: () =>
              Promise.resolve({ id: 'user', email: 'user@example.com', firstName: 'User', lastName: 'Test', defaultCurrency: 'BRL' }),
          },
        },
        { provide: WalletItemService, useValue: walletItems },
        { provide: ImportService, useValue: importService },
        { provide: GroupService, useValue: groupService },
      ],
    });
  });

  it('loads catalogs and resolves the contextual category by concept', async () => {
    const store = TestBed.inject(CsvImportCatalogStore);
    await store.load();

    expect(store.defaultCurrency).toBe('BRL');
    expect(store.currencyOptions).toEqual(['BRL']);
    expect(store.findKnownCurrency('Real brasileiro')).toBe('BRL');
    expect(store.findKnownCurrency('R$')).toBeUndefined();
    expect(store.categoriesFor('group')).toHaveLength(1);
    expect(store.findMatchingCategory({ groupId: 'group' } as never, store.categories[0])?.id).toBe('group-food');
    expect(importService.categoryCatalog).toHaveBeenCalledOnce();
  });

  it('caches group members and adds the current user when missing', async () => {
    const store = TestBed.inject(CsvImportCatalogStore);
    await store.load();

    expect(await store.ensureGroupMembers('group')).toEqual([expect.objectContaining({ id: 'user' })]);
    await store.ensureGroupMembers('group');
    expect(groupService.findAllMembers).not.toHaveBeenCalled();
  });

  it('pages wallet items but preloads complete unpaged category catalogs', async () => {
    walletItems.getAllItems.mockImplementation(({ page }: { page: number }) =>
      Promise.resolve({
        content: page === 0 ? Array.from({ length: 10 }, (_, index) => ({ id: `wallet-${index}` })) : [{ id: 'wallet-10' }],
      }),
    );

    const store = TestBed.inject(CsvImportCatalogStore);
    await store.load();

    expect(store.walletItems).toHaveLength(11);
    expect(walletItems.getAllItems).toHaveBeenNthCalledWith(1, { size: 10, page: 0, sort: 'name' });
    expect(walletItems.getAllItems).toHaveBeenNthCalledWith(2, { size: 10, page: 1, sort: 'name' });
    expect(importService.categoryCatalog).toHaveBeenCalledOnce();
  });
});

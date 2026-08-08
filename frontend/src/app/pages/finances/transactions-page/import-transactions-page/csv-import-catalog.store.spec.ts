import { TestBed } from '@angular/core/testing';

import { of } from 'rxjs';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CurrencyCatalogService } from '../../../../components/currency-selector/currency-catalog.service';
import { UserService } from '../../../../services/user.service';
import { GroupCategoriesService } from '../../services/group-categories.service';
import { GroupService } from '../../services/group.service';
import { UserCategoriesService } from '../../services/user-categories.service';
import { WalletItemService } from '../../services/wallet-item.service';
import { CsvImportCatalogStore } from './csv-import-catalog.store';

describe('CsvImportCatalogStore', () => {
  const groupCategories = { getAllCategories: vi.fn() };
  const groupService = { getAllGroups: vi.fn(), findAllMembers: vi.fn() };

  beforeEach(() => {
    groupCategories.getAllCategories.mockResolvedValue({ content: [{ id: 'group-food', name: 'Alimentação', conceptId: 'food' }] });
    groupService.getAllGroups.mockResolvedValue([{ id: 'group', name: 'Casa' }]);
    groupService.findAllMembers.mockResolvedValue([]);
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
        { provide: WalletItemService, useValue: { getAllItems: () => Promise.resolve({ content: [{ id: 'wallet' }] }) } },
        {
          provide: UserCategoriesService,
          useValue: {
            getAllCategories: () => Promise.resolve({ content: [{ id: 'personal-food', name: 'Alimentação', conceptId: 'food' }] }),
          },
        },
        { provide: GroupCategoriesService, useValue: groupCategories },
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
  });

  it('caches group members and adds the current user when missing', async () => {
    const store = TestBed.inject(CsvImportCatalogStore);
    await store.load();

    expect(await store.ensureGroupMembers('group')).toEqual([expect.objectContaining({ id: 'user' })]);
    await store.ensureGroupMembers('group');
    expect(groupService.findAllMembers).toHaveBeenCalledTimes(1);
  });
});

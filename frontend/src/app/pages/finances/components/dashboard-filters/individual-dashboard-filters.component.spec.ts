import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { IndividualDashboardFiltersComponent } from './individual-dashboard-filters.component';

describe('IndividualDashboardFiltersComponent', () => {
  const groupServiceMock = {
    searchGroups: vi.fn().mockResolvedValue({
      content: [
        { id: 'group-1', name: 'Group 1' },
        { id: 'group-2', name: 'Group 2' },
      ],
    }),
  };
  const bankAccountServiceMock = {
    getAllBankAccount: vi.fn().mockResolvedValue({
      content: [{ id: 'bank-1', name: 'Bank 1' }],
    }),
  };
  const creditCardServiceMock = {
    getAllCreditCards: vi.fn().mockResolvedValue({
      content: [{ id: 'card-1', name: 'Card 1' }],
    }),
  };
  const userCategoriesServiceMock = {
    getAllCategories: vi.fn().mockResolvedValue({
      content: [
        { id: 'category-1', name: 'Category 1' },
        { id: 'category-2', name: 'Category 2' },
      ],
    }),
  };

  const injector = Injector.create({ providers: [] });
  let component: IndividualDashboardFiltersComponent;

  beforeEach(() => {
    vi.clearAllMocks();
    component = runInInjectionContext(
      injector,
      () =>
        new IndividualDashboardFiltersComponent(
          groupServiceMock as never,
          bankAccountServiceMock as never,
          creditCardServiceMock as never,
          userCategoriesServiceMock as never,
        ),
    );
  });

  it('loads dashboard options through paged getters', async () => {
    await expect(component.groupOptionsGetter(0, 'abc')).resolves.toEqual([
      { id: 'group-1', label: 'Group 1' },
      { id: 'group-2', label: 'Group 2' },
    ]);
    expect(groupServiceMock.searchGroups).toHaveBeenCalledWith({ page: 0, size: 10, sort: 'name' }, 'abc');

    await expect(component.bankAccountOptionsGetter(1, 'bank')).resolves.toEqual([{ id: 'bank-1', label: 'Bank 1' }]);
    expect(bankAccountServiceMock.getAllBankAccount).toHaveBeenCalledWith({ page: 1, size: 10, sort: 'name' }, 'bank');

    await expect(component.creditCardOptionsGetter(2, 'card')).resolves.toEqual([{ id: 'card-1', label: 'Card 1' }]);
    expect(creditCardServiceMock.getAllCreditCards).toHaveBeenCalledWith({ page: 2, size: 10, sort: 'name' }, 'card');

    await expect(component.categoryOptionsGetter(3, 'cat')).resolves.toEqual([
      { id: 'category-1', label: 'Category 1' },
      { id: 'category-2', label: 'Category 2' },
    ]);
    expect(userCategoriesServiceMock.getAllCategories).toHaveBeenCalledWith(
      { onlyRoot: false, mountChildren: false, query: 'cat' },
      { page: 3, size: 10, sort: 'name' },
    );
  });
});

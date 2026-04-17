import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { IndividualDashboardFiltersComponent } from './individual-dashboard-filters.component';

describe('IndividualDashboardFiltersComponent', () => {
  const groupServiceMock = {
    getAllGroups: vi.fn().mockResolvedValue([
      { id: 'group-b', name: 'Group B' },
      { id: 'group-a', name: 'Group A' },
    ]),
  };
  const bankAccountServiceMock = {
    getAllBankAccount: vi.fn().mockResolvedValue({
      content: [
        { id: 'bank-b', name: 'Bank B' },
        { id: 'bank-a', name: 'Bank A' },
      ],
    }),
  };
  const creditCardServiceMock = {
    getAllCreditCards: vi.fn().mockResolvedValue({
      content: [
        { id: 'card-b', name: 'Card B' },
        { id: 'card-a', name: 'Card A' },
      ],
    }),
  };

  const injector = Injector.create({ providers: [] });
  let component: IndividualDashboardFiltersComponent;

  beforeEach(async () => {
    vi.clearAllMocks();
    component = runInInjectionContext(
      injector,
      () =>
        new IndividualDashboardFiltersComponent(groupServiceMock as never, bankAccountServiceMock as never, creditCardServiceMock as never),
    );
    await Promise.resolve();
    await Promise.resolve();
  });

  it('loads and sorts option lists for dashboard filters', () => {
    expect(groupServiceMock.getAllGroups).toHaveBeenCalledOnce();
    expect(bankAccountServiceMock.getAllBankAccount).toHaveBeenCalledOnce();
    expect(creditCardServiceMock.getAllCreditCards).toHaveBeenCalledOnce();
    expect(component.groupOptions.map(item => item.id)).toEqual(['group-a', 'group-b']);
    expect(component.bankAccountOptions.map(item => item.id)).toEqual(['bank-a', 'bank-b']);
    expect(component.creditCardOptions.map(item => item.id)).toEqual(['card-a', 'card-b']);
  });
});

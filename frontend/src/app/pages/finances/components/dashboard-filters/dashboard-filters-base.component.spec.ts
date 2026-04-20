import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { beforeEach, describe, expect, it } from 'vitest';

import { DashboardFiltersBaseComponent } from './dashboard-filters-base.component';

describe('DashboardFiltersBaseComponent', () => {
  const injector = Injector.create({
    providers: [
      {
        provide: TranslateService,
        useValue: {
          instant(key: string) {
            return key;
          },
        },
      },
    ],
  });
  let component: DashboardFiltersBaseComponent;

  beforeEach(() => {
    component = runInInjectionContext(injector, () => new DashboardFiltersBaseComponent());
  });

  it('serializes empty filters by default', () => {
    expect(component.serializeFilters()).toEqual({
      groupIds: [],
      memberIds: [],
      creditCardIds: [],
      bankAccountIds: [],
      entryTypes: [],
      categoryIds: [],
      includeUncategorized: false,
    });
  });

  it('serializes combined filters from reactive form', () => {
    component.form.setValue({
      groupIds: ['group-1', 'group-2'],
      memberIds: [],
      creditCardIds: ['card-1'],
      bankAccountIds: ['bank-1'],
      entryTypes: ['EXPENSE', 'TRANSFER'],
      categorySelectionIds: ['category-1', 'category-2'],
    });

    expect(component.serializeFilters()).toEqual({
      groupIds: ['group-1', 'group-2'],
      memberIds: [],
      creditCardIds: ['card-1'],
      bankAccountIds: ['bank-1'],
      entryTypes: ['EXPENSE', 'TRANSFER'],
      categoryIds: ['category-1', 'category-2'],
      includeUncategorized: false,
    });
  });

  it('serializes uncategorized selection separately from category ids', () => {
    component.form.setValue({
      groupIds: [],
      memberIds: [],
      creditCardIds: [],
      bankAccountIds: [],
      entryTypes: [],
      categorySelectionIds: ['__UNCATEGORIZED_OPTION__', 'category-1'],
    });

    expect(component.serializeFilters()).toEqual({
      groupIds: [],
      memberIds: [],
      creditCardIds: [],
      bankAccountIds: [],
      entryTypes: [],
      categoryIds: ['category-1'],
      includeUncategorized: true,
    });
  });
});

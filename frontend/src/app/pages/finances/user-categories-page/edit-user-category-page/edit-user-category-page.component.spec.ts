import '@angular/compiler';
import { FormBuilder } from '@angular/forms';

import { Subject } from 'rxjs';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CUSTOM_CATEGORY_CONCEPT_OPTION_ID } from '../../services/category-concept-form.util';
import { EditUserCategoryPageComponent } from './edit-user-category-page.component';

describe('EditUserCategoryPageComponent', () => {
  const formBuilder = new FormBuilder();
  const routeParamMap$ = new Subject<any>();

  const routerMock = {
    navigate: vi.fn().mockResolvedValue(true),
    navigateByUrl: vi.fn().mockResolvedValue(true),
  };
  const categoriesServiceMock = {
    editCategory: vi.fn().mockResolvedValue({}),
    getCategory: vi.fn(),
    getAvailableConcepts: vi.fn(),
    deleteCategory: vi.fn(),
  };
  const errorMessageServiceMock = { handleError: vi.fn() };
  const confirmationServiceMock = { confirm: vi.fn() };
  const translateServiceMock = { instant: (key: string) => key };

  let component: EditUserCategoryPageComponent;

  beforeEach(() => {
    vi.clearAllMocks();
    component = new EditUserCategoryPageComponent(
      formBuilder,
      {} as never,
      {} as never,
      routerMock as never,
      { paramMap: routeParamMap$ } as never,
      categoriesServiceMock as never,
      errorMessageServiceMock as never,
      confirmationServiceMock as never,
      translateServiceMock as never,
    );

    component.category = {
      id: 'category-1',
      name: 'Category',
      color: '#ffffff',
      parentId: null,
      conceptId: 'concept-1',
      children: [],
    };
    component.formGroup = formBuilder.group({
      name: ['Updated category'],
      parent: [null],
      color: ['#000000'],
      conceptId: [CUSTOM_CATEGORY_CONCEPT_OPTION_ID],
      customConceptName: ['  trips  '],
    });
  });

  it('submits custom concept payload on edit', async () => {
    component.isDebtSfCategory = false;

    await component.submit();

    expect(categoriesServiceMock.editCategory).toHaveBeenCalledWith('category-1', {
      name: 'Updated category',
      color: '#000000',
      parentId: undefined,
      conceptId: null,
      customConceptName: 'trips',
    });
    expect(routerMock.navigate).toHaveBeenCalledOnce();
  });

  it('blocks delete confirmation when category is DEBT_SF', async () => {
    component.isDebtSfCategory = true;

    await component.askForConfirmationToDelete();
    expect(confirmationServiceMock.confirm).not.toHaveBeenCalled();

    component.isDebtSfCategory = false;
    await component.askForConfirmationToDelete();
    expect(confirmationServiceMock.confirm).toHaveBeenCalledOnce();
  });
});

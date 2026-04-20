import '@angular/compiler';
import { FormBuilder } from '@angular/forms';

import { Subject } from 'rxjs';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CUSTOM_CATEGORY_CONCEPT_OPTION_ID } from '../../services/category-concept-form.util';
import { EditGroupCategoryPageComponent } from './edit-group-category-page.component';

describe('EditGroupCategoryPageComponent', () => {
  const formBuilder = new FormBuilder();
  const routeParamMap$ = new Subject<any>();

  const routerMock = {
    navigate: vi.fn().mockResolvedValue(true),
    navigateByUrl: vi.fn().mockResolvedValue(true),
  };
  const groupCategoriesServiceMock = {
    editCategory: vi.fn().mockResolvedValue({}),
    getCategory: vi.fn(),
    getAvailableConcepts: vi.fn(),
    deleteCategory: vi.fn(),
  };
  const errorMessageServiceMock = { handleError: vi.fn() };
  const confirmationServiceMock = { confirm: vi.fn() };
  const translateServiceMock = { instant: (key: string) => key };

  let component: EditGroupCategoryPageComponent;

  beforeEach(() => {
    vi.clearAllMocks();
    component = new EditGroupCategoryPageComponent(
      formBuilder,
      {} as never,
      {} as never,
      routerMock as never,
      { paramMap: routeParamMap$ } as never,
      groupCategoriesServiceMock as never,
      errorMessageServiceMock as never,
      confirmationServiceMock as never,
      translateServiceMock as never,
    );

    component.groupId = 'group-1';
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
      customConceptName: ['  groceries  '],
    });
  });

  it('submits custom concept payload on group category edit', async () => {
    component.isDebtSfCategory = false;

    await component.submit();

    expect(groupCategoriesServiceMock.editCategory).toHaveBeenCalledWith('group-1', 'category-1', {
      name: 'Updated category',
      color: '#000000',
      parentId: undefined,
      conceptId: null,
      customConceptName: 'groceries',
    });
    expect(routerMock.navigate).toHaveBeenCalledOnce();
  });

  it('blocks delete confirmation when group category is DEBT_SF', async () => {
    component.isDebtSfCategory = true;

    await component.askForConfirmationToDelete();
    expect(confirmationServiceMock.confirm).not.toHaveBeenCalled();

    component.isDebtSfCategory = false;
    await component.askForConfirmationToDelete();
    expect(confirmationServiceMock.confirm).toHaveBeenCalledOnce();
  });
});

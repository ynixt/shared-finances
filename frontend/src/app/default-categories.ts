import { TranslateService } from '@ngx-translate/core';

import { NewCategoryDto } from './models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';

export const defaultCategories: NewCategoryDto[] = [
  {
    name: 'defaultCategories.vehicle',
    color: '#2980B9',
    parentId: null,
  },
  {
    name: 'defaultCategories.wellBeing',
    color: '#8E44AD',
    parentId: null,
  },
  {
    name: 'defaultCategories.home',
    color: '#1ABC9C',
    parentId: null,
  },
  {
    name: 'defaultCategories.pets',
    color: '#F1C40F',
    parentId: null,
  },
  {
    name: 'defaultCategories.supermarket',
    color: '#27AE60',
    parentId: null,
  },
  {
    name: 'defaultCategories.education',
    color: '#E67E22',
    parentId: null,
  },
  {
    name: 'defaultCategories.family',
    color: '#2ECC71',
    parentId: null,
  },
  {
    name: 'defaultCategories.wage',
    color: '#2ca05f',
    parentId: null,
  },
  {
    name: 'defaultCategories.fees',
    color: '#34495E',
    parentId: null,
  },
  {
    name: 'defaultCategories.fun',
    color: '#9B59B6',
    parentId: null,
  },
  {
    name: 'defaultCategories.personal',
    color: '#16A085',
    parentId: null,
  },
  {
    name: 'defaultCategories.health',
    color: '#48C9B0',
    parentId: null,
  },
  {
    name: 'defaultCategories.transport',
    color: '#5DADE2',
    parentId: null,
  },
  {
    name: 'defaultCategories.clothing',
    color: '#F39C12',
    parentId: null,
  },
];

export const getDefaultCategoriesTranslated = (translateService: TranslateService) => {
  return defaultCategories.map(cat => ({
    ...cat,
    name: translateService.instant(cat.name),
  }));
};

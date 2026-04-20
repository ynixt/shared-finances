import { TranslateService } from '@ngx-translate/core';

import { CategoryConceptDto, NewCategoryDto } from './models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';

type DefaultCategoryTemplate = {
  name: string;
  color: string;
  parentId: null;
  conceptCode: string;
};

export const defaultCategories: DefaultCategoryTemplate[] = [
  {
    name: 'defaultCategories.vehicle',
    color: '#2980B9',
    parentId: null,
    conceptCode: 'VEHICLE',
  },
  {
    name: 'defaultCategories.wellBeing',
    color: '#8E44AD',
    parentId: null,
    conceptCode: 'WELL_BEING',
  },
  {
    name: 'defaultCategories.home',
    color: '#1ABC9C',
    parentId: null,
    conceptCode: 'HOME',
  },
  {
    name: 'defaultCategories.pets',
    color: '#F1C40F',
    parentId: null,
    conceptCode: 'PETS',
  },
  {
    name: 'defaultCategories.supermarket',
    color: '#27AE60',
    parentId: null,
    conceptCode: 'SUPERMARKET',
  },
  {
    name: 'defaultCategories.education',
    color: '#E67E22',
    parentId: null,
    conceptCode: 'EDUCATION',
  },
  {
    name: 'defaultCategories.family',
    color: '#2ECC71',
    parentId: null,
    conceptCode: 'FAMILY',
  },
  {
    name: 'defaultCategories.wage',
    color: '#2ca05f',
    parentId: null,
    conceptCode: 'WAGE',
  },
  {
    name: 'defaultCategories.fees',
    color: '#34495E',
    parentId: null,
    conceptCode: 'FEES',
  },
  {
    name: 'defaultCategories.fun',
    color: '#9B59B6',
    parentId: null,
    conceptCode: 'ENTERTAINMENT',
  },
  {
    name: 'defaultCategories.personal',
    color: '#16A085',
    parentId: null,
    conceptCode: 'PERSONAL',
  },
  {
    name: 'defaultCategories.health',
    color: '#48C9B0',
    parentId: null,
    conceptCode: 'HEALTH',
  },
  {
    name: 'defaultCategories.transport',
    color: '#5DADE2',
    parentId: null,
    conceptCode: 'TRANSPORT',
  },
  {
    name: 'defaultCategories.clothing',
    color: '#F39C12',
    parentId: null,
    conceptCode: 'CLOTHING',
  },
  {
    name: 'defaultCategories.debtSf',
    color: '#f31261',
    parentId: null,
    conceptCode: 'DEBT_SF',
  },
];

export const getDefaultCategoriesTranslated = (translateService: TranslateService, concepts: CategoryConceptDto[]): NewCategoryDto[] => {
  const conceptIdByCode = new Map(concepts.filter(concept => concept.code != null).map(concept => [concept.code as string, concept.id]));

  return defaultCategories.map(cat => ({
    color: cat.color,
    name: translateService.instant(cat.name),
    parentId: cat.parentId,
    conceptId: requireConceptId(cat.conceptCode, conceptIdByCode),
  }));
};

const requireConceptId = (conceptCode: string, conceptIdByCode: Map<string, string>): string => {
  const conceptId = conceptIdByCode.get(conceptCode);
  if (conceptId == null) {
    throw new Error(`Missing required category concept code: ${conceptCode}`);
  }
  return conceptId;
};

import { describe, expect, it } from 'vitest';

import {
  CUSTOM_CATEGORY_CONCEPT_OPTION_ID,
  isCustomCategoryConceptOption,
  isDebtSfConcept,
  resolveCategoryConceptPayload,
} from './category-concept-form.util';

describe('category-concept-form.util', () => {
  it('detects custom option id', () => {
    expect(isCustomCategoryConceptOption(CUSTOM_CATEGORY_CONCEPT_OPTION_ID)).toBe(true);
    expect(isCustomCategoryConceptOption('regular-concept')).toBe(false);
  });

  it('resolves payload for existing concept', () => {
    expect(resolveCategoryConceptPayload('concept-1', 'ignored')).toEqual({
      conceptId: 'concept-1',
      customConceptName: null,
    });
  });

  it('resolves payload for custom concept', () => {
    expect(resolveCategoryConceptPayload(CUSTOM_CATEGORY_CONCEPT_OPTION_ID, '  My custom label  ')).toEqual({
      conceptId: null,
      customConceptName: 'My custom label',
    });
  });

  it('detects DEBT_SF concept', () => {
    expect(isDebtSfConcept({ id: '1', kind: 'PREDEFINED', code: 'DEBT_SF', displayName: null })).toBe(true);
    expect(isDebtSfConcept({ id: '2', kind: 'PREDEFINED', code: 'SUPERMARKET', displayName: null })).toBe(false);
  });
});

import { CategoryConceptDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';

export const CUSTOM_CATEGORY_CONCEPT_OPTION_ID = '__CUSTOM_CATEGORY_CONCEPT__';
export const DEBT_SF_CONCEPT_CODE = 'DEBT_SF';

export function isDebtSfConcept(concept: CategoryConceptDto | null | undefined): boolean {
  return concept?.code === DEBT_SF_CONCEPT_CODE;
}

export function isCustomCategoryConceptOption(conceptId: string | null | undefined): boolean {
  return conceptId === CUSTOM_CATEGORY_CONCEPT_OPTION_ID;
}

export function resolveCategoryConceptPayload(
  conceptId: string | null | undefined,
  customConceptName: string | null | undefined,
): { conceptId?: string | null; customConceptName?: string | null } {
  const normalizedCustomName = customConceptName?.trim() ?? '';

  if (isCustomCategoryConceptOption(conceptId)) {
    return {
      conceptId: null,
      customConceptName: normalizedCustomName.length > 0 ? normalizedCustomName : null,
    };
  }

  return {
    conceptId: conceptId ?? null,
    customConceptName: null,
  };
}

export function formatConceptCodeLabel(code: string): string {
  return code
    .split('_')
    .map(part => part.charAt(0) + part.slice(1).toLowerCase())
    .join(' ');
}

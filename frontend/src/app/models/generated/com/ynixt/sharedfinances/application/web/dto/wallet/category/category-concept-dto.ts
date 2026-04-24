/* eslint-disable */
/* tslint-disable */
import { WalletCategoryConceptCode } from '../../../../../domain/enums/wallet-category-concept-code';
import { WalletCategoryConceptKind } from '../../../../../domain/enums/wallet-category-concept-kind';

export interface CategoryConceptDto {
  code?: WalletCategoryConceptCode | null;
  displayName?: string | null;
  id: string;
  kind: WalletCategoryConceptKind;
}

/* eslint-disable */
/* tslint-disable */

import { CategoryDto } from '../wallet/category/category-dto';
import { ImportGroupCategoryCatalogDto } from './import-group-category-catalog-dto';

export interface ImportCategoryCatalogDto {
  groups: Array<ImportGroupCategoryCatalogDto>;
  personal: Array<CategoryDto>;
}

/* eslint-disable */
/* tslint-disable */

import { CategoryDto } from '../wallet/category/category-dto';
import { UserSimpleDto } from '../user/user-simple-dto';

export interface ImportGroupCategoryCatalogDto {
  categories: Array<CategoryDto>;
  groupId: string;
  members: Array<UserSimpleDto>;
}

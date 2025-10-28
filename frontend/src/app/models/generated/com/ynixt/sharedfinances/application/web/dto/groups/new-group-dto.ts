/* eslint-disable */
/* tslint-disable */
import { NewCategoryDto } from '../wallet/category/new-category-dto';

export interface NewGroupDto {
  categories?: Array<NewCategoryDto> | null;
  name: string;
}

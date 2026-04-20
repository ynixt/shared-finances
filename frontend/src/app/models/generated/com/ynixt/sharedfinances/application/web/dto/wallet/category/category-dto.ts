/* eslint-disable */
/* tslint-disable */

export interface CategoryDto {
  children?: Array<CategoryDto> | null;
  color: string;
  conceptId: string;
  id: string;
  name: string;
  parentId?: string | null;
}

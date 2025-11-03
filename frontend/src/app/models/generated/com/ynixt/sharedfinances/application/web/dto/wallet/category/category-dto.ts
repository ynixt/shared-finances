/* tslint-disable */

export interface CategoryDto {
  children?: Array<CategoryDto> | null;
  color: string;
  id: string;
  name: string;
  parentId?: string | null;
}

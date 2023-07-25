import { Observable } from 'rxjs';
import { Category } from 'src/app/@core/models';

export interface GroupWithIdName {
  id: string;
  name: string;
}

export abstract class GenericCategoryService {
  abstract watchCategories(groupId?: string): Observable<Category[]>;
  abstract newCategory(category: Partial<Category>, groupId?: string): Observable<Category>;
  abstract editCategory(category: Category): Observable<Category>;
  abstract getById(categoryId: string, groupId?: string): Promise<Category>;
  abstract deleteCategory(categoryId: string): Observable<void>;
  abstract getGroup(groupId: string): Promise<GroupWithIdName | null>;
}

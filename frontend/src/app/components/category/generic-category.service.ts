import { Observable } from 'rxjs';
import { Category } from 'src/app/@core/models';

export abstract class GenericCategoryService {
  abstract watchCategories(): Observable<Category[]>;
  abstract newCategory(category: Partial<Category>): Observable<Category>;
  abstract editCategory(category: Category): Observable<Category>;
  abstract getById(categoryId: string): Promise<Category>;
  abstract deleteCategory(categoryId: string): Observable<boolean>;
}

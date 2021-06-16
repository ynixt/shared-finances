import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { UserCategoryRoutingModule } from './user-category-routing.module';
import { UserCategoryComponent } from './user-category.component';
import { SharedModule } from 'src/app/shared';
import { CategoryModule, GenericCategoryService, GENERIC_CATEGORY_URL_TOKEN } from 'src/app/components/category';
import { UserCategoryService } from 'src/app/@core/services';

@NgModule({
  declarations: [UserCategoryComponent],
  providers: [
    { provide: GenericCategoryService, useClass: UserCategoryService },
    { provide: GENERIC_CATEGORY_URL_TOKEN, useValue: '/finances/single/category' },
  ],
  imports: [CommonModule, SharedModule, UserCategoryRoutingModule, CategoryModule],
})
export class UserCategoryModule {}

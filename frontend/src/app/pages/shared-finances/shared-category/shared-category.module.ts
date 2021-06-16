import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SharedCategoryRoutingModule } from './shared-category-routing.module';
import { SharedCategoryComponent } from './shared-category.component';
import { SharedModule } from 'src/app/shared';
import { CategoryModule, GenericCategoryService, GENERIC_CATEGORY_URL_TOKEN } from 'src/app/components/category';
import { UserCategoryService } from 'src/app/@core/services';

@NgModule({
  declarations: [SharedCategoryComponent],
  providers: [
    { provide: GenericCategoryService, useClass: UserCategoryService },
    { provide: GENERIC_CATEGORY_URL_TOKEN, useValue: '/finances/shared/category' },
  ],
  imports: [CommonModule, SharedModule, SharedCategoryRoutingModule, CategoryModule],
})
export class SharedCategoryModule {}

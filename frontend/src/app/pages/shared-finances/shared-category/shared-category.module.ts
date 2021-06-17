import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SharedCategoryRoutingModule } from './shared-category-routing.module';
import { SharedCategoryComponent } from './shared-category.component';
import { SharedModule } from 'src/app/shared';
import { CategoryModule, GenericCategoryService, GENERIC_CATEGORY_URL_TOKEN } from 'src/app/components/category';
import { SharedCategoryService } from './shared-category.service';

@NgModule({
  declarations: [SharedCategoryComponent],
  providers: [
    { provide: GenericCategoryService, useClass: SharedCategoryService },
    { provide: GENERIC_CATEGORY_URL_TOKEN, useValue: '/finances/shared/:groupId/category' },
  ],
  imports: [CommonModule, SharedModule, SharedCategoryRoutingModule, CategoryModule],
})
export class SharedCategoryModule {}

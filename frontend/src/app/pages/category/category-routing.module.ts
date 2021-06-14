import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CategoryComponent } from './category.component';
import { EditCategoryComponent } from './edit-category/edit-category.component';
import { NewCategoryComponent } from './new-category/new-category.component';

const routes: Routes = [
  {
    path: '',
    component: CategoryComponent,
  },
  {
    path: 'new',
    component: NewCategoryComponent,
  },
  {
    path: ':id/edit',
    component: EditCategoryComponent,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class CategoryRoutingModule {}

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { EditCategoryComponent, NewCategoryComponent } from 'src/app/components/category';
import { SharedCategoryComponent } from './shared-category.component';

const routes: Routes = [
  {
    path: '',
    component: SharedCategoryComponent,
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
export class SharedCategoryRoutingModule {}

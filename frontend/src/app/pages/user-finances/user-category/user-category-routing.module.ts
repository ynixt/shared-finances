import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { EditCategoryComponent, NewCategoryComponent } from 'src/app/components/category';
import { UserCategoryComponent } from './user-category.component';

const routes: Routes = [
  {
    path: '',
    component: UserCategoryComponent,
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
export class UserCategoryRoutingModule {}

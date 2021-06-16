import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { EditCategoryComponent, NewCategoryComponent } from 'src/app/components/category';
import { TitleGuard } from 'src/app/shared';
import { SharedCategoryComponent } from './shared-category.component';

const routes: Routes = [
  {
    path: '',
    component: SharedCategoryComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'categories',
    },
  },
  {
    path: 'new',
    component: NewCategoryComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'new-category',
    },
  },
  {
    path: ':id/edit',
    component: EditCategoryComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'edit-category',
    },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class SharedCategoryRoutingModule {}

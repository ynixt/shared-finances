import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PermissionGuard } from 'src/app/shared/permission.guard';

const routes: Routes = [
  {
    path: 'category',
    loadChildren: () => import('./user-category/user-category.module').then(m => m.UserCategoryModule),
    canActivate: [PermissionGuard],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class UserFinancesRoutingModule {}

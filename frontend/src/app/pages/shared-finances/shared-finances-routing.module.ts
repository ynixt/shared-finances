import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TitleGuard } from 'src/app/shared';
import { GroupsComponent, GroupSinglePageComponent, EditGroupComponent } from './groups';
import { InviteComponent } from './invite';

const routes: Routes = [
  {
    path: '',
    component: GroupsComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'groups',
    },
  },
  {
    path: ':groupId/edit',
    component: EditGroupComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'edit-group',
    },
  },
  {
    path: 'invite/:inviteId',
    component: InviteComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'invite',
    },
  },
  {
    path: ':id',
    component: GroupSinglePageComponent,
    canActivate: [TitleGuard],
  },
  {
    path: ':groupId/category',
    loadChildren: () => import('./shared-category/shared-category.module').then(m => m.SharedCategoryModule),
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class SharedFinancesRoutingModule {}

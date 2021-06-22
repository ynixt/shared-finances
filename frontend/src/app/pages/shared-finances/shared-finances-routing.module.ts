import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TitleGuard } from 'src/app/shared';
import { SharedFinancesComponent } from '.';
import { GroupsComponent, GroupSinglePageComponent } from './groups';
import { InviteComponent } from './invite';

const routes: Routes = [
  {
    path: '',
    component: SharedFinancesComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'shared',
    },
  },
  {
    path: 'groups',
    component: GroupsComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'groups',
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

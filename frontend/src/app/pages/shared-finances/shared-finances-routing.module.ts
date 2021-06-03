import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedFinancesComponent } from '.';
import { GroupsComponent, GroupSinglePageComponent } from './groups';
import { InviteComponent } from './invite';

const routes: Routes = [
  {
    path: '',
    component: SharedFinancesComponent,
  },
  {
    path: 'groups',
    component: GroupsComponent,
  },
  {
    path: ':id',
    component: GroupSinglePageComponent,
  },
  {
    path: 'invite/:inviteId',
    component: InviteComponent,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class SharedFinancesRoutingModule {}

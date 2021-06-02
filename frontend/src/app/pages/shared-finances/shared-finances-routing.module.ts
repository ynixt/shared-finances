import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedFinancesComponent } from '.';
import { GroupsComponent, GroupSinglePageComponent } from './groups';

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
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class SharedFinancesRoutingModule {}

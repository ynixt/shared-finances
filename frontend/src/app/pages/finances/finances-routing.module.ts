import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard.component';
import { FinancesComponent } from './finances.component';

const routes: Routes = [
  {
    path: '',
    component: FinancesComponent,
    children: [
      {
        path: '',
        component: DashboardComponent,
      },
      {
        path: 'shared',
        loadChildren: () => import('../shared-finances/shared-finances.module').then(m => m.SharedFinancesModule),
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class FinancesRoutingModule {}

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
        path: 'single',
        loadChildren: () => import('../user-finances/user-finances.module').then(m => m.UserFinancesModule),
      },
      {
        path: 'shared',
        loadChildren: () => import('../shared-finances/shared-finances.module').then(m => m.SharedFinancesModule),
      },
      {
        path: 'credit',
        loadChildren: () => import('../credit-card/credit-card.module').then(m => m.CreditCardModule),
      },
      {
        path: 'bank',
        loadChildren: () => import('../bank-account/bank-account.module').then(m => m.BankAccountModule),
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class FinancesRoutingModule {}

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { NotFoundComponent } from './pages/errors';
import { PermissionGuard } from './shared/permission.guard';

const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./pages/home/home.module').then(m => m.HomeModule),
  },
  {
    path: 'auth',
    loadChildren: () => import('./pages/auth/auth.module').then(m => m.AuthModule),
  },
  {
    path: 'finances',
    loadChildren: () => import('./pages/finances/finances.module').then(m => m.FinancesModule),
    canActivate: [PermissionGuard],
  },
  {
    path: 'credit',
    loadChildren: () => import('./pages/credit-card/credit-card.module').then(m => m.CreditCardModule),
    canActivate: [PermissionGuard],
  },
  {
    path: 'bank',
    loadChildren: () => import('./pages/bank-account/bank-account.module').then(m => m.BankAccountModule),
    canActivate: [PermissionGuard],
  },
  {
    path: '404',
    component: NotFoundComponent,
  },
  {
    path: '**',
    redirectTo: '/404',
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}

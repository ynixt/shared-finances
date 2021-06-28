import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TitleGuard } from 'src/app/shared';
import { BankAccountSingleComponent } from './bank-account-single/bank-account-single.component';
import { BankAccountComponent } from './bank-account.component';
import { NewBankAccountComponent } from './new-bank-account/new-bank-account.component';

const routes: Routes = [
  {
    'path': '',
    component: BankAccountComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'bank-accounts',
    },
  },
  {
    'path': 'new',
    component: NewBankAccountComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'new-bank-account',
    },
  },
  {
    'path': ':bankAccountId',
    component: BankAccountSingleComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'bank-account',
    },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BankAccountRoutingModule {}

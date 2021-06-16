import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TitleGuard } from 'src/app/shared';
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
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BankAccountRoutingModule {}

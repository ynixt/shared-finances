import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BankAccountComponent } from './bank-account.component';
import { NewBankAccountComponent } from './new-bank-account/new-bank-account.component';

const routes: Routes = [
  {
    'path': '',
    component: BankAccountComponent,
  },
  {
    'path': 'new',
    component: NewBankAccountComponent,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BankAccountRoutingModule {}

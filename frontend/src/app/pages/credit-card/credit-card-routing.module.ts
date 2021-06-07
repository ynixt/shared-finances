import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CreditCardComponent } from './credit-card.component';
import { NewCreditCardComponent } from './new-credit-card/new-credit-card.component';

const routes: Routes = [
  {
    path: '',
    component: CreditCardComponent,
  },
  {
    path: 'new',
    component: NewCreditCardComponent,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class CreditCardRoutingModule {}

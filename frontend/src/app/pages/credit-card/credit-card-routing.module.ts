import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CreditCardComponent } from './credit-card.component';
import { EditCreditCardComponent } from './edit-credit-card/edit-credit-card.component';
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
  {
    path: ':id/edit',
    component: EditCreditCardComponent,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class CreditCardRoutingModule {}

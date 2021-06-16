import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TitleGuard } from 'src/app/shared';
import { CreditCardComponent } from './credit-card.component';
import { EditCreditCardComponent } from './edit-credit-card/edit-credit-card.component';
import { NewCreditCardComponent } from './new-credit-card/new-credit-card.component';

const routes: Routes = [
  {
    path: '',
    component: CreditCardComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'credit-cards',
    },
  },
  {
    path: 'new',
    component: NewCreditCardComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'new-credit-card',
    },
  },
  {
    path: ':id/edit',
    component: EditCreditCardComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'edit-credit-card',
    },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class CreditCardRoutingModule {}

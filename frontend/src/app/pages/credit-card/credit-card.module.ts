import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { CurrencyMaskModule } from 'ng2-currency-mask';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';

import { CreditCardRoutingModule } from './credit-card-routing.module';
import { CreditCardComponent } from './credit-card.component';
import { SharedModule } from 'src/app/shared/shared.module';
import { NewCreditCardComponent } from './new-credit-card/new-credit-card.component';
import { FormCreditCardComponent } from './form-credit-card/form-credit-card.component';
import { MatMenuModule } from '@angular/material/menu';
import { CovalentDialogsModule } from '@covalent/core/dialogs';
import { EditCreditCardComponent } from './edit-credit-card/edit-credit-card.component';
import { CreditCardSingleComponent } from './credit-card-single/credit-card-single.component';
import { OptionsBarModule } from 'src/app/components/options-bar';
import { TransactionsTableModule } from 'src/app/components/transactions-table/transactions-table.module';
import { NgxChartsModule } from '@swimlane/ngx-charts';

@NgModule({
  declarations: [CreditCardComponent, NewCreditCardComponent, FormCreditCardComponent, EditCreditCardComponent, CreditCardSingleComponent],
  imports: [
    CommonModule,
    CreditCardRoutingModule,
    SharedModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    ReactiveFormsModule,
    CurrencyMaskModule,
    MatMenuModule,
    CovalentDialogsModule,
    OptionsBarModule,
    TransactionsTableModule,
    NgxChartsModule,
    MatCheckboxModule,
  ],
})
export class CreditCardModule {}

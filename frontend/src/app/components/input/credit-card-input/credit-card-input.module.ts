import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { CreditCardInputComponent } from './credit-card-input.component';
import { TranslocoModule } from '@ngneat/transloco';
import { ReactiveFormsModule } from '@angular/forms';

@NgModule({
  declarations: [CreditCardInputComponent],
  imports: [CommonModule, TranslocoModule, MatFormFieldModule, MatInputModule, MatSelectModule, ReactiveFormsModule],
  exports: [CreditCardInputComponent],
})
export class CreditCardInputModule {}

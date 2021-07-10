import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslocoModule } from '@ngneat/transloco';

import { BankAccountInputComponent } from './bank-account-input.component';

@NgModule({
  declarations: [BankAccountInputComponent],
  imports: [CommonModule, TranslocoModule, MatFormFieldModule, MatInputModule, MatSelectModule, ReactiveFormsModule],
  exports: [BankAccountInputComponent],
})
export class BankAccountInputModule {}

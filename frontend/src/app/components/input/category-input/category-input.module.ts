import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CategoryInputComponent } from './category-input.component';
import { TranslocoModule } from '@ngneat/transloco';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from "@angular/material/select";

@NgModule({
  declarations: [CategoryInputComponent],
  imports: [CommonModule, TranslocoModule, MatFormFieldModule, MatInputModule, MatSelectModule, ReactiveFormsModule],
  exports: [CategoryInputComponent],
})
export class CategoryInputModule {}

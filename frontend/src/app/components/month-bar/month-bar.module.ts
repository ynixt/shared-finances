import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MonthBarComponent } from './month-bar.component';
import { SharedModule } from 'src/app/shared';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { FormsModule } from '@angular/forms';

@NgModule({
  declarations: [MonthBarComponent],
  imports: [CommonModule, SharedModule, FormsModule, MatDatepickerModule],
  exports: [MonthBarComponent],
})
export class MonthBarModule {}

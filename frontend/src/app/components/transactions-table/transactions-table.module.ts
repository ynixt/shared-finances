import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TransactionsTableComponent } from './transactions-table.component';
import { SharedModule } from 'src/app/shared';
import { MatMenuModule } from '@angular/material/menu';

@NgModule({
  declarations: [TransactionsTableComponent],
  imports: [CommonModule, SharedModule, MatMenuModule],
  exports: [TransactionsTableComponent],
})
export class TransactionsTableModule {}

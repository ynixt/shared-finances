import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TransactionsTableComponent } from './transactions-table.component';
import { SharedModule } from 'src/app/shared';
import { MatMenuModule } from '@angular/material/menu';
import { CovalentPagingModule } from '@covalent/core/paging';

@NgModule({
  declarations: [TransactionsTableComponent],
  imports: [CommonModule, SharedModule, MatMenuModule, CovalentPagingModule],
  exports: [TransactionsTableComponent],
})
export class TransactionsTableModule {}

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NoItemComponent } from './no-item.component';
import { RouterModule } from '@angular/router';
import { SharedLibsModule } from '../shared-libs';

@NgModule({
  declarations: [NoItemComponent],
  imports: [CommonModule, RouterModule, SharedLibsModule],
  exports: [NoItemComponent],
})
export class NoItemModule {}

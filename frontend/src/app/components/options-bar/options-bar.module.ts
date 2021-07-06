import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OptionsBarComponent } from './options-bar.component';
import { SharedModule } from 'src/app/shared';
import { MatMenuModule } from '@angular/material/menu';

@NgModule({
  declarations: [OptionsBarComponent],
  imports: [CommonModule, SharedModule, MatMenuModule],
  exports: [OptionsBarComponent],
})
export class OptionsBarModule {}

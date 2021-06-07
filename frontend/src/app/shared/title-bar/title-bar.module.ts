import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TitleBarComponent } from './title-bar.component';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';

@NgModule({
  declarations: [TitleBarComponent],
  imports: [CommonModule, MatIconModule, MatDividerModule],
  exports: [TitleBarComponent],
})
export class TitleBarModule {}

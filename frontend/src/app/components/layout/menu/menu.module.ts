import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatMenuModule } from '@angular/material/menu';

import { SharedModule } from 'src/app/shared/shared.module';
import { MenuComponent } from './menu.component';
@NgModule({
  declarations: [MenuComponent],
  imports: [CommonModule, SharedModule, RouterModule, MatExpansionModule, MatMenuModule],
  exports: [MenuComponent],
})
export class MenuModule {}

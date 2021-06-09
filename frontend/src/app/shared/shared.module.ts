import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SharedLibsModule } from './shared-libs';
import { TitleBarModule } from './title-bar/title-bar.module';
import { NoItemModule } from './no-item/no-item.module';

const layoutModules = [SharedLibsModule, TitleBarModule, NoItemModule];

@NgModule({
  declarations: [],
  imports: [CommonModule, ...layoutModules],
  exports: [...layoutModules],
})
export class SharedModule {}

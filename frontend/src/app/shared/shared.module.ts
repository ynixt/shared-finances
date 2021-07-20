import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SharedLibsModule } from './shared-libs';
import { TitleBarModule } from './title-bar/title-bar.module';
import { NoItemModule } from './no-item/no-item.module';
import { CurrencySymbolPipe, SpaceOnSignalPipe } from './pipes';

const layoutModules = [SharedLibsModule, TitleBarModule, NoItemModule];
const pipes = [SpaceOnSignalPipe, CurrencySymbolPipe];

@NgModule({
  declarations: [...pipes],
  imports: [CommonModule, ...layoutModules],
  exports: [...layoutModules, ...pipes],
})
export class SharedModule {}

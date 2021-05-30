import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StoreModule } from '@ngrx/store';
import { EffectsModule } from '@ngrx/effects';

import { reducers, effects, storeServices } from './';

@NgModule({
  imports: [CommonModule, StoreModule.forRoot(reducers), EffectsModule.forRoot(effects)],
  providers: [...storeServices],
})
export class AppStoreModule {}

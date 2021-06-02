import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedFinancesComponent } from './shared-finances.component';
import { SharedFinancesRoutingModule } from './shared-finances-routing.module';
import { SharedModule } from 'src/app/shared/shared.module';
import { GroupsModule } from './groups';

@NgModule({
  declarations: [SharedFinancesComponent],
  imports: [CommonModule, SharedFinancesRoutingModule, SharedModule, GroupsModule],
})
export class SharedFinancesModule {}

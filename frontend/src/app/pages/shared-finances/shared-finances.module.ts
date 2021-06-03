import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedFinancesComponent } from './shared-finances.component';
import { SharedFinancesRoutingModule } from './shared-finances-routing.module';
import { SharedModule } from 'src/app/shared/shared.module';
import { GroupsModule } from './groups';
import { InviteComponent } from './invite/invite.component';

@NgModule({
  declarations: [SharedFinancesComponent, InviteComponent],
  imports: [CommonModule, SharedFinancesRoutingModule, SharedModule, GroupsModule],
})
export class SharedFinancesModule {}

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedFinancesRoutingModule } from './shared-finances-routing.module';
import { SharedModule } from 'src/app/shared/shared.module';
import { GroupsModule } from './groups';
import { InviteComponent } from './invite/invite.component';

@NgModule({
  declarations: [InviteComponent],
  imports: [CommonModule, SharedFinancesRoutingModule, SharedModule, GroupsModule],
})
export class SharedFinancesModule {}

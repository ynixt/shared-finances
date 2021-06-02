import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from 'src/app/shared/shared.module';
import { GroupsComponent } from './groups.component';
import { GroupsService } from './groups.service';
import { GroupSinglePageComponent } from './group-single-page/group-single-page.component';
import { RouterModule } from '@angular/router';

@NgModule({
  declarations: [GroupsComponent, GroupSinglePageComponent],
  imports: [CommonModule, RouterModule, SharedModule],
  providers: [GroupsService],
  exports: [GroupsComponent, GroupSinglePageComponent],
})
export class GroupsModule {}

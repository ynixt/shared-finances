import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from 'src/app/shared/shared.module';
import { GroupsComponent } from './groups.component';
import { GroupSinglePageComponent } from './group-single-page/group-single-page.component';
import { RouterModule } from '@angular/router';
import { CovalentDialogsModule } from '@covalent/core/dialogs';
import { MatMenuModule } from '@angular/material/menu';

@NgModule({
  declarations: [GroupsComponent, GroupSinglePageComponent],
  imports: [CommonModule, RouterModule, SharedModule, CovalentDialogsModule, MatMenuModule],
  exports: [GroupsComponent, GroupSinglePageComponent],
})
export class GroupsModule {}

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from 'src/app/shared/shared.module';
import { GroupsComponent } from './groups.component';
import { GroupSinglePageComponent } from './group-single-page/group-single-page.component';
import { RouterModule } from '@angular/router';
import { CovalentDialogsModule } from '@covalent/core/dialogs';
import { MatMenuModule } from '@angular/material/menu';
import { NewGroupComponent } from './new-group/new-group.component';
import { FormGroupComponent } from './form-group/form-group.component';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { EditGroupComponent } from './edit-group/edit-group.component';
import { MonthBarModule } from 'src/app/components/month-bar';
import { TransactionsTableModule } from 'src/app/components/transactions-table/transactions-table.module';
import { GroupSummaryDashboardComponent } from './group-summary-dashboard/group-summary-dashboard.component';

@NgModule({
  declarations: [
    GroupsComponent,
    GroupSinglePageComponent,
    NewGroupComponent,
    FormGroupComponent,
    EditGroupComponent,
    GroupSummaryDashboardComponent,
  ],
  imports: [
    CommonModule,
    RouterModule,
    SharedModule,
    CovalentDialogsModule,
    MatMenuModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MonthBarModule,
    TransactionsTableModule,
  ],
  exports: [GroupsComponent, GroupSinglePageComponent],
})
export class GroupsModule {}

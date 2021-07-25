import { Component, Input, OnInit } from '@angular/core';
import { GroupSummary, GroupSummaryExpense, User } from 'src/app/@core/models';
import { Group } from 'src/app/@core/models/group';

@Component({
  selector: 'app-group-summary-dashboard',
  templateUrl: './group-summary-dashboard.component.html',
  styleUrls: ['./group-summary-dashboard.component.scss'],
})
export class GroupSummaryDashboardComponent implements OnInit {
  @Input() users: User[];
  @Input() groupSummary: GroupSummary;
  @Input() isLoading = false;

  constructor() {}

  ngOnInit(): void {}

  getExpenseFromUser(user: User): GroupSummaryExpense | undefined {
    const expense = this.groupSummary?.expenses?.find(expense => expense.userId === user.id);

    return (
      expense ?? {
        expense: 0,
        percentageOfExpenses: 0,
        userId: user.id,
      }
    );
  }
}

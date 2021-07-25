import { Component, Input, OnInit } from '@angular/core';
import { GroupSummary } from 'src/app/@core/models';
import { Group } from 'src/app/@core/models/group';

@Component({
  selector: 'app-group-summary-dashboard',
  templateUrl: './group-summary-dashboard.component.html',
  styleUrls: ['./group-summary-dashboard.component.scss'],
})
export class GroupSummaryDashboardComponent implements OnInit {
  @Input() groupSummary: GroupSummary;
  @Input() isLoading = false;

  constructor() {}

  ngOnInit(): void {}
}

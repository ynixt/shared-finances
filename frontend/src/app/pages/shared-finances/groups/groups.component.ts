import { Component, OnInit } from '@angular/core';
import { Group } from 'src/app/@core/models/group';
import { GroupsService } from 'src/app/@core/services/groups.service';

@Component({
  selector: 'app-groups',
  templateUrl: './groups.component.html',
  styleUrls: ['./groups.component.scss'],
})
export class GroupsComponent implements OnInit {
  groups$: Promise<Group[]>;

  constructor(private groupsService: GroupsService) {}

  ngOnInit(): void {
    this.groups$ = this.groupsService.getGroups();
  }
}

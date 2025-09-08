import { SlicePipe } from '@angular/common';
import { Component, ViewEncapsulation, computed, effect, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Avatar } from 'primeng/avatar';
import { Skeleton } from 'primeng/skeleton';

import { UserAvatarComponent, UserAvatarSize, convertUserAvatarSizeToRem } from '../../../../components/user-avatar/user-avatar.component';
import { GroupUserDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { GroupService } from '../../services/group.service';

@Component({
  selector: 'app-group-user-list',
  imports: [UserAvatarComponent, Avatar, RouterLink, SlicePipe, Skeleton],
  templateUrl: './group-user-list.component.html',
  styleUrl: './group-user-list.component.scss',
  encapsulation: ViewEncapsulation.None,
})
export class GroupUserListComponent {
  loading = false;
  members: GroupUserDto[] = [];

  size = input<UserAvatarSize>('large');
  sizeInRem = computed(() => {
    return convertUserAvatarSizeToRem(this.size());
  });

  groupId = input<string | undefined>(undefined);
  limitBeforeMoreMembers = input<number>(4);

  constructor(private groupService: GroupService) {
    effect(async () => {
      this.loading = true;

      const groupId = this.groupId();

      if (groupId == null) {
        this.members = [];
      } else {
        this.members = await this.groupService.findAllMembers(groupId);
      }

      this.loading = false;
    });
  }
}

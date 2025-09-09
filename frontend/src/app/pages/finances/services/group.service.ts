import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import {
  ChangeRoleGroupUserRequestDto,
  GroupDto,
  GroupUserDto,
  GroupWithRoleDto,
  NewGroupDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { UserGroupRole } from '../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({
  providedIn: 'root',
})
export class GroupService {
  constructor(
    private http: HttpClient,
    private userService: UserService,
  ) {}

  async getAllGroups(): Promise<Array<GroupWithRoleDto>> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.get<Array<GroupWithRoleDto>>('/api/groups').pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async getGroup(groupId: string): Promise<GroupWithRoleDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.get<GroupWithRoleDto>(`/api/groups/${groupId}`).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async newGroup(newGroupDto: NewGroupDto): Promise<GroupDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.post<GroupDto>('/api/groups', newGroupDto).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async findAllMembers(groupId: string): Promise<Array<GroupUserDto>> {
    const user = await this.userService.getUser();

    if (user != null) {
      return (await lastValueFrom(this.http.get<Array<GroupUserDto>>(`/api/groups/${groupId}/members`).pipe(take(1)))).sort((a, b) =>
        a.user.firstName.localeCompare(b.user.firstName),
      );
    }

    throw new UserMissingError();
  }

  async updateMemberRole(groupId: string, memberId: string, newRole: UserGroupRole): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      const request: ChangeRoleGroupUserRequestDto = {
        memberId,
        role: newRole,
      };

      await lastValueFrom(this.http.put<void>(`/api/groups/${groupId}/members/change-role`, request).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }
}

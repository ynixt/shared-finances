import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { lastValueFrom, take } from 'rxjs';

import { getDefaultCategoriesTranslated } from '../../../default-categories';
import {
  ChangeRoleGroupUserRequestDto,
  EditGroupDto,
  GroupDto,
  GroupUserDto,
  GroupWithRoleDto,
  NewGroupDto,
  UpdateGroupPlanningSimulatorOptInDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { CategoryConceptDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { UserGroupRole } from '../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { Page, PageRequest } from '../../../models/pagination';
import { PaginationService } from '../../../services/pagination.service';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({
  providedIn: 'root',
})
export class GroupService {
  constructor(
    private http: HttpClient,
    private userService: UserService,
    private translateService: TranslateService,
    private paginationService: PaginationService,
  ) {}

  async getAllGroups(): Promise<Array<GroupWithRoleDto>> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.get<Array<GroupWithRoleDto>>('/api/groups').pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async searchGroups(request?: PageRequest, query?: string): Promise<Page<GroupWithRoleDto>> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(
        this.paginationService.get<GroupWithRoleDto>('/api/groups/search', request, { query: query?.trim() || undefined }).pipe(take(1)),
      );
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

  async editGroup(groupId: string, request: EditGroupDto): Promise<GroupWithRoleDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.put<GroupWithRoleDto>(`/api/groups/${groupId}`, request).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async deleteGroup(groupId: string): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.delete<void>(`/api/groups/${groupId}`).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async newGroup(newGroupDto: NewGroupDto, useDefaultCategories = true): Promise<GroupDto> {
    const user = await this.userService.getUser();

    if (user == null) {
      throw new UserMissingError();
    }

    if (useDefaultCategories) {
      const concepts = await lastValueFrom(this.http.get<CategoryConceptDto[]>('/api/categories/concepts').pipe(take(1)));
      newGroupDto.categories = getDefaultCategoriesTranslated(this.translateService, concepts);
    }

    return lastValueFrom(this.http.post<GroupDto>('/api/groups', newGroupDto).pipe(take(1)));
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

  async updateOwnPlanningSimulatorOptIn(groupId: string, allowPlanningSimulator: boolean): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      const request: UpdateGroupPlanningSimulatorOptInDto = {
        allowPlanningSimulator,
      };

      await lastValueFrom(this.http.put<void>(`/api/groups/${groupId}/members/me/planning-simulator-opt-in`, request).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }
}

import { Injectable } from '@nestjs/common';
import { AuthenticationError } from 'apollo-server-errors';

import { Group } from '../models/group';
import { UserService } from '../user/user.service';
import { UpdateGroupArgs } from '../models/args';
import { GroupRepository } from './group.repository';
import { GroupInviteRepository } from './group-invite.repository';

@Injectable()
export class GroupService {
  public static instance: GroupService;

  constructor(
    private userService: UserService,
    private groupRepository: GroupRepository,
    private groupInviteRepository: GroupInviteRepository,
  ) {
    GroupService.instance = this;
  }

  public getGroupsByUserId(userId: string): Promise<Group[]> {
    return this.groupRepository.getAllByUserId(userId);
  }

  public getGroup(userId: string, groupId: string): Promise<Group> {
    return this.groupRepository.getByUserIdGroupId(userId, groupId);
  }

  public getGroupWithoutCheckPermission(groupId: string): Promise<Group> {
    return this.groupRepository.getById(groupId);
  }

  public async updateGroup(userId: string, newGroup: UpdateGroupArgs): Promise<Group | null> {
    return this.groupRepository.update(userId, newGroup);
  }

  public async generateInvite(userId: string, groupId: string): Promise<string> {
    const userAllowed = await this.groupRepository.groupHasUser(userId, { groupId });

    if (!userAllowed) {
      throw new AuthenticationError('');
    }

    await this.deleteOldInvites();

    return this.groupInviteRepository.createAndGetId(groupId);
  }

  public async deleteOldInvites(): Promise<void> {
    const limitDate = new Date(new Date().getTime() - 24 * 60 * 60 * 1000).toISOString();
    await this.groupInviteRepository.deleteWithCreationDateLess(limitDate);
  }

  public async useInvite(userId: string, inviteId: string): Promise<string | null> {
    const invite = await this.groupInviteRepository.getInviteWithGroup(inviteId);

    const { group } = invite;

    if ((await this.groupRepository.groupHasUser(userId, { group })) == false) {
      return null;
    }

    await this.groupRepository.runInsideTransaction(async opts => {
      await this.userService.addGroupToUser(userId, group.id, opts);
      await this.groupRepository.addUserToGroup(group.id, userId, opts);
      await this.groupInviteRepository.deleteById(inviteId, opts);
    });

    return group.id;
  }

  public userHasAccessToGroup(userId: string, groupId: string): Promise<boolean> {
    return this.groupRepository.groupHasUser(userId, { groupId });
  }
}

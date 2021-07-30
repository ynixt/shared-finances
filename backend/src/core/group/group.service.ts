import { Injectable } from '@nestjs/common';
import { AuthenticationError } from 'apollo-server-errors';

import { Group } from '../models/group';
import { UserService } from '../user/user.service';
import { NewGroupArgs, UpdateGroupArgs } from '../models/args';
import { GroupRepository } from './group.repository';
import { GroupInviteRepository } from './group-invite.repository';
import { FBUser } from '../auth/firebase-strategy';

@Injectable()
export class GroupService {
  constructor(
    private userService: UserService,
    private groupRepository: GroupRepository,
    private groupInviteRepository: GroupInviteRepository,
  ) {}

  public getGroupsByUserId(userId: string): Promise<Group[]> {
    return this.groupRepository.getAllByUserId(userId);
  }

  public getGroup(userId: string, groupId: string): Promise<Group> {
    return this.groupRepository.getByUserIdGroupId(userId, groupId);
  }

  public getGroupWithoutCheckPermission(groupId: string): Promise<Group> {
    return this.groupRepository.getById(groupId);
  }

  public async newGroup(user: FBUser, newGroup: NewGroupArgs): Promise<Group | null> {
    return this.groupRepository.runInsideTransaction(async opts => {
      const createdGroup = await this.groupRepository.create(
        {
          usersId: [user.id],
          name: newGroup.name,
        },
        opts,
      );

      await this.userService.addGroupToUser(user.id, createdGroup.id);

      return createdGroup;
    });
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

    if (invite != null) {
      const { group } = invite;

      if ((await this.groupRepository.groupHasUser(userId, { group })) == true) {
        return null;
      }

      await this.groupRepository.runInsideTransaction(async opts => {
        await this.userService.addGroupToUser(userId, group.id, opts);
        await this.groupRepository.addUserToGroup(group.id, userId, opts);
        await this.groupInviteRepository.deleteById(inviteId, opts);
      });
      return group.id;
    }

    return null;
  }

  public async userHasAccessToGroup(userId: string, groupId: string): Promise<boolean> {
    if (userId == null || groupId == null) {
      return false;
    }

    return this.groupRepository.groupHasUser(userId, { groupId });
  }

  getUsersIdFromGroup(groupId: string): Promise<string[]> {
    return this.groupRepository.getUsersIdFromGroup(groupId);
  }
}

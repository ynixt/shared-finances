import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { AuthenticationError } from 'apollo-server-errors';
import { Model } from 'mongoose';
import { GroupShareUrl, GroupShareUrlDocument } from '../models';
import { Group, GroupDocument } from '../models/group';
import { UserService } from '../user/user.service';
import * as mongoose from 'mongoose';
import { UpdateGroupArgs } from '../models/args';

@Injectable()
export class GroupService {
  public static instance: GroupService;

  constructor(
    @InjectModel(Group.name) private groupModel: Model<GroupDocument>,
    @InjectModel(GroupShareUrl.name) private groupShareUrlModel: Model<GroupShareUrlDocument>,
    private userService: UserService,
  ) {
    GroupService.instance = this;
  }

  public async getGroupsByUserId(userId: string): Promise<Group[]> {
    const groups = await this.groupModel.find({ users: { $elemMatch: { $eq: userId } } });

    return groups;
  }

  public async getGroup(userId: string, groupId: string): Promise<Group> {
    const group = await this.groupModel
      .findOne({
        $and: [
          {
            _id: groupId,
          },
          { users: { $elemMatch: { $eq: userId } } },
        ],
      })
      .exec();

    return group;
  }

  public async updateGroup(userId: string, newGroup: UpdateGroupArgs): Promise<Group | null> {
    const updateResponse = await this.groupModel.updateOne(
      {
        $and: [
          {
            _id: newGroup.id,
          },
          { users: { $elemMatch: { $eq: userId } } },
        ],
      },
      { name: newGroup.name },
    );

    if (updateResponse.n > 0) {
      return this.getGroup(userId, newGroup.id);
    }

    return null;
  }

  public async generateShareUrl(userId: string, groupId: string): Promise<string> {
    const userAllowed = await this.userHasAccessToGroup(userId, groupId);

    if (!userAllowed) {
      throw new AuthenticationError('');
    }

    this.deleteOldShareUrl();

    const shareUrl = await this.groupShareUrlModel.create({
      group: groupId,
      creationDate: new Date().toISOString(),
    });

    return shareUrl.id;
  }

  public async useInvite(userId: string, inviteId: string): Promise<string | null> {
    const invite = await this.groupShareUrlModel.findById(inviteId).populate('group').exec();

    if (invite == null) {
      return null;
    }

    const { group } = invite;

    if (group.users.filter(user => new mongoose.Types.ObjectId(user.id).toHexString() === userId).length > 0) {
      return null;
    }

    const session = await this.groupShareUrlModel.db.startSession();

    try {
      await this.userService.addGroupToUser(userId, group.id);
      await this.addUserToGroup(group.id, userId);
      await this.groupShareUrlModel.deleteOne({ _id: inviteId });
    } catch (err) {
      session.abortTransaction();
      throw err;
    } finally {
      session.endSession();
    }

    return group.id;
  }

  async addUserToGroup(groupId: string, userId): Promise<void> {
    await this.groupModel.updateOne(
      { _id: groupId },
      {
        $addToSet: {
          users: userId,
        },
      },
    );
  }

  public userHasAccessToGroup(userId: string, groupId: string): Promise<boolean> {
    return this.groupModel.exists({ $and: [{ _id: groupId }, { users: { $elemMatch: { $eq: userId } } }] });
  }

  private deleteOldShareUrl() {
    const limitDate = new Date(new Date().getTime() - 24 * 60 * 60 * 1000).toISOString();

    this.groupShareUrlModel.deleteMany({ creationDate: { $lt: limitDate } });
  }
}

import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { AuthenticationError } from 'apollo-server-errors';
import { Model } from 'mongoose';
import { GroupShareUrl, GroupShareUrlDocument } from '../models';
import { Group, GroupDocument } from '../models/group';
import { UserService } from '../user/user.service';
import * as mongoose from 'mongoose';

@Injectable()
export class GroupService {
  constructor(
    @InjectModel(Group.name) private groupModel: Model<GroupDocument>,
    @InjectModel(GroupShareUrl.name) private groupShareUrlModel: Model<GroupShareUrlDocument>,
    private userService: UserService,
  ) {}

  public async getGroupsByUserId(userId: string): Promise<Group[]> {
    const groups = await this.groupModel.find({ users: { $elemMatch: { $eq: userId } } });

    return groups;
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

  private userHasAccessToGroup(userId: string, groupId: string): Promise<boolean> {
    return this.groupModel.exists({ $and: [{ _id: groupId }, { users: { $elemMatch: { $eq: userId } } }] });
  }

  private deleteOldShareUrl() {
    const limitDate = new Date(new Date().getTime() - 24 * 60 * 60 * 1000).toISOString();

    this.groupShareUrlModel.deleteMany({ creationDate: { $lt: limitDate } });
  }
}

import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { AuthenticationError } from 'apollo-server-errors';
import { Model } from 'mongoose';
import { GroupShareUrl, GroupShareUrlDocument } from '../models';
import { Group, GroupDocument } from '../models/group';

@Injectable()
export class GroupService {
  constructor(
    @InjectModel(Group.name) private groupModel: Model<GroupDocument>,
    @InjectModel(GroupShareUrl.name) private groupShareUrlModel: Model<GroupShareUrlDocument>,
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

  private userHasAccessToGroup(userId: string, groupId: string): Promise<boolean> {
    return this.groupModel.exists({ $and: [{ _id: groupId }, { users: { $elemMatch: { $eq: userId } } }] });
  }

  private deleteOldShareUrl() {
    const limitDate = new Date(new Date().getTime() - 24 * 60 * 60 * 1000).toISOString();

    this.groupShareUrlModel.deleteMany({ creationDate: { $lt: limitDate } });
  }
}

import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Group, GroupDocument } from '../models/group';

@Injectable()
export class GroupService {
  constructor(@InjectModel(Group.name) private groupModel: Model<GroupDocument>) {}

  public async getGroupsByUserId(userId: string): Promise<Group[]> {
    const groups = await this.groupModel.find({ users: { $elemMatch: { $eq: userId } } });

    return groups;
  }
}

import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Group, GroupDocument } from '../models';
import { UpdateGroupArgs } from '../models/args';
import { MongoDefaultRepository } from '../data';
import { MongoRepositoryOptions } from '../data/mongo-repository';

@Injectable()
export class GroupRepository extends MongoDefaultRepository<Group, GroupDocument> {
  constructor(@InjectModel(Group.name) groupModel: Model<GroupDocument>) {
    super(groupModel);
  }

  public async getAllByUserId(userId: string): Promise<Group[]> {
    const groups = await this.model.find({ usersId: { $elemMatch: { $eq: userId } } });

    return groups;
  }

  public async getByUserIdGroupId(userId: string, groupId: string): Promise<Group> {
    const group = await this.model
      .findOne({
        $and: [
          {
            _id: groupId,
          },
          { usersId: { $elemMatch: { $eq: userId } } },
        ],
      })
      .exec();

    return group;
  }

  public async update(userId: string, newGroup: UpdateGroupArgs): Promise<Group | null> {
    const updateResponse = await this.model.updateOne(
      {
        $and: [
          {
            _id: newGroup.id,
          },
          { usersId: { $elemMatch: { $eq: userId } } },
        ],
      },
      { name: newGroup.name },
    );

    if (updateResponse.n > 0) {
      return this.getByUserIdGroupId(userId, newGroup.id);
    }

    return null;
  }

  async addUserToGroup(groupId: string, userId, opts?: MongoRepositoryOptions): Promise<void> {
    await this.model.updateOne(
      { _id: groupId },
      {
        $addToSet: {
          usersId: userId,
        },
      },
      opts,
    );
  }

  async getUsersIdFromGroup(groupId: string, opts?: MongoRepositoryOptions): Promise<string[]> {
    const group = await this.model.findById(groupId, undefined, opts).select('usersId').exec();

    return group ? group.usersId.map(userObjectId => (userObjectId as unknown as Types.ObjectId).toHexString()) : [];
  }

  public groupHasUser(userId: string, options: { groupId: string }): Promise<boolean>;
  public groupHasUser(userId: string, options: { group: Group }): Promise<boolean>;

  public async groupHasUser(userId: string, options: { groupId?: string; group?: Group }): Promise<boolean> {
    if (options.groupId != null) {
      return this.model.exists({ $and: [{ _id: options.groupId }, { usersId: { $elemMatch: { $eq: userId } } }] });
    }

    if (options.group != null) {
      if (options.group.users != null) {
        return options.group.users.find(user => new Types.ObjectId(userId).toHexString() === user.id) != null;
      }

      return options.group.usersId.find(userGroupId => new Types.ObjectId(userId).toHexString() === userGroupId) != null;
    }

    return null;
  }
}

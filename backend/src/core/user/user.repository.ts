import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';

import { MongoDefaultRepository } from '../data';
import { MongoRepositoryOptions } from '../data/mongo-repository';
import { User, UserDocument } from '../models';

@Injectable()
export class UserRepository extends MongoDefaultRepository<User, UserDocument> {
  constructor(@InjectModel(User.name) groupModel: Model<UserDocument>) {
    super(groupModel);
  }

  public async getByUid(uid: string): Promise<User> {
    const user = await this.model.findOne({ uid }).exec();

    return user;
  }

  public async addGroupToUser(userId: string, groupId: string, opts?: MongoRepositoryOptions): Promise<void> {
    await this.model.updateOne(
      { _id: userId },
      {
        $addToSet: {
          groups: groupId,
        },
      },
      opts,
    );
  }
}

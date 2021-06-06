import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';

import { MongoRepository } from '../data';
import { User, UserDocument } from '../models';

@Injectable()
export class UserRepository extends MongoRepository<User, UserDocument> {
  constructor(@InjectModel(User.name) groupModel: Model<UserDocument>) {
    super(groupModel);
  }

  public async create(uid: string): Promise<User> {
    const createdUser = await this.model.create({
      uid: uid,
    });

    return this.geById(createdUser.id);
  }

  public async geById(id: string): Promise<User> {
    const user = await this.model.findById(id);

    return user;
  }

  public async getByUid(uid: string): Promise<User> {
    const user = await this.model.findOne({ uid });

    return user;
  }

  public async addGroupToUser(userId: string, groupId: string): Promise<void> {
    await this.model.updateOne(
      { _id: userId },
      {
        $addToSet: {
          groups: groupId,
        },
      },
    );
  }
}

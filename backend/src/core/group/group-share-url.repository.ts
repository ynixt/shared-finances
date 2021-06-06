import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { GroupShareUrl, GroupShareUrlDocument } from '../models';
import { MongoRepository } from '../data';

@Injectable()
export class GroupShareUrlRepository extends MongoRepository<GroupShareUrl, GroupShareUrlDocument> {
  constructor(@InjectModel(GroupShareUrl.name) groupShareUrlModel: Model<GroupShareUrlDocument>) {
    super(groupShareUrlModel);
  }

  public async createAndGetId(groupId: string): Promise<string> {
    const shareUrl = await this.model.create({
      group: groupId,
      creationDate: new Date().toISOString(),
    });

    return shareUrl.id;
  }

  public async getInviteWithGroup(inviteId: string) {
    const invite = await this.model.findById(inviteId).populate('group').exec();

    if (invite == null) {
      return null;
    }

    return invite;
  }

  deleteById(inviteId: string): any {
    return this.model.deleteOne({ _id: inviteId });
  }

  public deleteWithCreationDateLess(limitDate: string): any {
    return this.model.deleteMany({ creationDate: { $lt: limitDate } });
  }
}

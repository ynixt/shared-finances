import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';

import { MongoDefaultRepository } from '../data';
import { BankAccount, BankAccountDocument } from '../models';

@Injectable()
export class BankAccountRepository extends MongoDefaultRepository<BankAccount, BankAccountDocument> {
  constructor(@InjectModel(BankAccount.name) userDocument: Model<BankAccountDocument>) {
    super(userDocument);
  }

  async changeName(userId: string, bankAccountId: string, newName: string): Promise<BankAccount> {
    const result = await this.model
      .findOneAndUpdate(
        { $and: [{ _id: bankAccountId }, { userId }] },
        {
          $set: {
            'name': newName,
          },
        },
        { new: true },
      )
      .exec();

    return result;
  }

  async delete(userId: string, bankAccountId: string): Promise<boolean> {
    const result = await this.model.deleteOne({ $and: [{ _id: bankAccountId }, { userId }] }).exec();

    return result.n > 0;
  }

  findAllWithUserId(userId: string): Promise<BankAccount[]> {
    return this.model.find({ userId }).exec();
  }

  existsWithUserId(userId: string, bankAccountId: string): Promise<boolean> {
    return this.model.exists({ $and: [{ _id: bankAccountId }, { userId }] });
  }
}

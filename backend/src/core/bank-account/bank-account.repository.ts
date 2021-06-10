import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';

import { MongoEmbededRepository } from '../data';
import { BankAccount, User, UserDocument } from '../models';
import { NewBankAccountArgs } from '../models/args';

@Injectable()
export class BankAccountRepository extends MongoEmbededRepository<BankAccount, User, UserDocument> {
  constructor(@InjectModel(User.name) userDocument: Model<UserDocument>) {
    super(userDocument);
  }

  async create(userId: string, domain: NewBankAccountArgs): Promise<BankAccount> {
    domain = { id: new Types.ObjectId().toHexString(), ...domain };

    const result = await this.model
      .findOneAndUpdate(
        { _id: userId },
        {
          $addToSet: {
            bankAccounts: domain,
          },
        },
        { new: true },
      )
      .exec();

    return result.bankAccounts.filter(bankAccount => bankAccount.id === domain.id)[0];
  }

  async changeName(userId: string, bankAccountId: string, newName: string): Promise<BankAccount> {
    const result = await this.model
      .findOneAndUpdate(
        { _id: userId, 'bankAccounts.id': bankAccountId },
        {
          $set: {
            'bankAccounts.$.name': newName,
          },
        },
        { new: true },
      )
      .exec();

    return result.bankAccounts.filter(bankAccount => bankAccount.id === bankAccountId)[0];
  }

  async delete(userId: string, bankAccountId: string): Promise<boolean> {
    const result = await this.model
      .updateOne(
        { _id: userId },
        {
          $pull: {
            bankAccounts: { id: bankAccountId },
          },
        },
        { new: true },
      )
      .exec();

    return result.n > 0;
  }
}

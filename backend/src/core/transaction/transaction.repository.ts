import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { MongoDefaultRepository } from '../data';
import { MongoRepositoryOptions } from '../data/mongo-repository';
import { Transaction, TransactionDocument } from '../models';

@Injectable()
export class TransactionRepository extends MongoDefaultRepository<Transaction, TransactionDocument> {
  constructor(@InjectModel(Transaction.name) transactionDocument: Model<TransactionDocument>) {
    super(transactionDocument);
  }

  async deleteByBankAccountId(bankAccountId: string, opts?: MongoRepositoryOptions): Promise<void> {
    await this.model.deleteMany({ bankAccountId }, opts);
  }

  async getBalanceByBankAccount(bankAccountId: string): Promise<number> {
    try {
      const result = await this.model
        .aggregate([
          {
            $match: {
              bankAccountId: new Types.ObjectId(bankAccountId),
            },
          },
          {
            $group: {
              _id: null,
              balance: { $sum: '$value' },
            },
          },
        ])
        .exec();

      return result.length === 1 ? result[0].balance : 0;
    } catch (err) {
      console.error(err);
    }
  }
}

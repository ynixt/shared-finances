import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
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
}

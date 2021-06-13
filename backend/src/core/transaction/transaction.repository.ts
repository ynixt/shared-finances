import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { MongoDefaultRepository } from '../data';
import { Transaction, TransactionDocument } from '../models';

@Injectable()
export class TransactionRepository extends MongoDefaultRepository<Transaction, TransactionDocument> {
  constructor(@InjectModel(Transaction.name) userDocument: Model<TransactionDocument>) {
    super(userDocument);
  }
}

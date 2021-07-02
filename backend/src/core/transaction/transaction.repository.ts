import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Pagination, PaginationService } from 'src/shared';
import { MongoDefaultRepository } from '../data';
import { MongoRepositoryOptions } from '../data/mongo-repository';
import { Transaction, TransactionDocument, TransactionsPage } from '../models';
import { EditTransactionArgs } from '../models/args';

@Injectable()
export class TransactionRepository extends MongoDefaultRepository<Transaction, TransactionDocument> {
  constructor(
    @InjectModel(Transaction.name) transactionDocument: Model<TransactionDocument>,
    private paginationService: PaginationService,
  ) {
    super(transactionDocument);
  }

  async edit(domain: EditTransactionArgs): Promise<Transaction> {
    const result = await this.model
      .findOneAndUpdate(
        { _id: domain.transactionId },
        {
          $set: {
            'transactionType': domain.transactionType,
            'date': domain.date,
            'value': domain.value,
            'description': domain.description,
            'bankAccountId': domain.bankAccountId,
            'creditCardId': domain.creditCardId,
            'categoryId': domain.categoryId,
            'groupId': domain.groupId,
            'userId': domain.firstUserId,
          },
        },
        { new: true, omitUndefined: true },
      )
      .exec();

    return result;
  }

  async deleteById(transactionId: string, opts?: MongoRepositoryOptions): Promise<boolean> {
    const result = await this.model.deleteOne({ _id: transactionId }, opts);

    return result.n > 0;
  }

  async deleteByBankAccountId(bankAccountId: string, opts?: MongoRepositoryOptions): Promise<void> {
    await this.model.deleteMany({ bankAccountId }, opts);
  }

  async getByBankAccountId(
    bankAccountId: string,
    args?: { maxDate?: string; minDate?: string },
    pagination = new Pagination(),
    opts?: MongoRepositoryOptions,
  ): Promise<TransactionsPage> {
    const query = this.model.find({ bankAccountId }, opts);

    if (args?.minDate) {
      query.and([{ date: { '$gte': args.minDate } }]);
    }

    if (args?.maxDate) {
      query.and([{ date: { '$lte': args.maxDate } }]);
    }

    query.sort({ date: -1 });

    return new TransactionsPage(await this.paginationService.convertQueryToPage(query, pagination));
  }

  async getBalanceByBankAccountId(bankAccountId: string, args?: { maxDate?: string }): Promise<number> {
    try {
      const aggregate = this.model.aggregate([
        {
          $match: {
            bankAccountId: new Types.ObjectId(bankAccountId),
          },
        },
      ]);

      if (args?.maxDate) {
        aggregate.match({
          date: { '$lt': args.maxDate },
        });
      }

      aggregate.group({
        _id: null,
        balance: { $sum: '$value' },
      });

      const result = await aggregate.exec();

      return result.length === 1 ? result[0].balance : 0;
    } catch (err) {
      console.error(err);
    }
  }
}

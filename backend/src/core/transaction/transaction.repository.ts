import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Pagination, PaginationService } from 'src/shared';
import { MongoDefaultRepository } from '../data';
import { MongoRepositoryOptions } from '../data/mongo-repository';
import { Transaction, TransactionDocument, TransactionsPage } from '../models';

@Injectable()
export class TransactionRepository extends MongoDefaultRepository<Transaction, TransactionDocument> {
  constructor(
    @InjectModel(Transaction.name) transactionDocument: Model<TransactionDocument>,
    private paginationService: PaginationService,
  ) {
    super(transactionDocument);
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

/**
 * Incluir botão para trocar mes/ano da conta bancária
 * esse mes/ano deve ser enviado como maior data possível no filtro do saldo
 * separar o saldo em uma req a parte
 */

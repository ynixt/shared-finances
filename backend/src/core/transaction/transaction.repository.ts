import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import moment from 'moment';
import { Model, Types } from 'mongoose';
import { Pagination, PaginationService } from 'src/shared';
import { MongoDefaultRepository } from '../data';
import { MongoRepositoryOptions } from '../data/mongo-repository';
import { BankAccountSummary, CreditCardSummary, Transaction, TransactionDocument, TransactionsPage } from '../models';
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
            'creditCardBillDate': domain.creditCardBillDate,
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

  async deleteByCreditCardId(creditCardId: string, opts?: MongoRepositoryOptions): Promise<void> {
    await this.model.deleteMany({ creditCardId }, opts);
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

  async getByCreditCardId(
    creditCardId: string,
    args?: { maxDate?: string; minDate?: string; creditCardBillDate?: string },
    pagination = new Pagination(),
    opts?: MongoRepositoryOptions,
  ): Promise<TransactionsPage> {
    const query = this.model.find({ creditCardId }, opts);

    if (args?.minDate) {
      query.and([{ date: { '$gte': args.minDate } }]);
    }

    if (args?.maxDate) {
      query.and([{ date: { '$lte': args.maxDate } }]);
    }

    if (args?.creditCardBillDate) {
      query.and([{ creditCardBillDate: args.creditCardBillDate }]);
    }

    query.sort({ date: -1 });

    return new TransactionsPage(await this.paginationService.convertQueryToPage(query, pagination));
  }

  async getBalanceByBankAccountId(bankAccountId: string, args?: { maxDate?: string }): Promise<number> {
    const aggregate = this.model.aggregate([
      {
        $match: {
          bankAccountId: new Types.ObjectId(bankAccountId),
        },
      },
    ]);

    if (args?.maxDate) {
      aggregate.match({
        date: { '$lte': args.maxDate },
      });
    }

    aggregate.group({
      _id: null,
      balance: { $sum: '$value' },
    });

    const result = await aggregate.exec();

    return result.length === 1 ? result[0].balance : 0;
  }
  async getBankAccountSummary(bankAccountId: string, args?: { maxDate?: string }): Promise<BankAccountSummary> {
    const aggregate = this.model.aggregate([
      {
        $match: {
          bankAccountId: new Types.ObjectId(bankAccountId),
        },
      },
    ]);

    if (args?.maxDate) {
      aggregate.match({
        date: { '$lte': args.maxDate },
      });
    }

    aggregate.project({
      date: 1,
      value: 1,
      transactionType: 1,
    });

    aggregate.addFields({
      valueThisMonth: {
        $cond: [
          {
            $and: [
              { $lt: ['$date', moment(args.maxDate).endOf('month').endOf('day').toISOString()] },
              { $gte: ['$date', moment(args.maxDate).startOf('month').startOf('day').toISOString()] },
            ],
          },
          '$value',
          0,
        ],
      },
    });

    aggregate.project({
      balance: '$value',
      expenses: {
        $cond: [
          {
            $in: ['$transactionType', ['Expense', 'CreditCardBillPayment']],
          },
          '$valueThisMonth',
          0,
        ],
      },
      revenues: {
        $cond: [
          {
            $eq: ['$transactionType', 'Revenue'],
          },
          '$valueThisMonth',
          0,
        ],
      },
    });

    aggregate.project({
      balance: 1,
      expenses: { $multiply: ['$expenses', -1] },
      revenues: 1,
    });

    aggregate.group({
      _id: null,
      balance: { $sum: '$balance' },
      expenses: { $sum: '$expenses' },
      revenues: { $sum: '$revenues' },
    });

    const result = await aggregate.exec();

    return result.length === 1 ? result[0] : null;
  }

  async getCreditCardSummary(creditCardId: string, maxCreditCardBillDate?: string): Promise<CreditCardSummary> {
    const aggregate = this.model.aggregate([
      {
        $match: {
          creditCardId: new Types.ObjectId(creditCardId),
        },
      },
    ]);

    if (maxCreditCardBillDate != null) {
      aggregate.match({
        creditCardBillDate: { $lte: maxCreditCardBillDate },
      });
    }

    aggregate.project({
      expenses: {
        $cond: [
          {
            $eq: ['$transactionType', 'CreditCard'],
          },
          '$value',
          0,
        ],
      },
      payments: {
        $cond: [
          {
            $eq: ['$transactionType', 'CreditCardBillPayment'],
          },
          '$value',
          0,
        ],
      },
      creditCardBillDate: '$creditCardBillDate',
    });

    aggregate.addFields({
      expensesOfThisBill: {
        $cond: [
          {
            $eq: ['$creditCardBillDate', maxCreditCardBillDate],
          },
          '$expenses',
          0,
        ],
      },
    });

    aggregate.group({
      _id: null,
      expenses: { $sum: '$expenses' },
      payments: { $sum: '$payments' },
      expensesOfThisBill: { $sum: '$expensesOfThisBill' },
    });

    aggregate.project({
      payments: { $multiply: ['$payments', -1] },
      expensesOfThisBill: { $multiply: ['$expensesOfThisBill', -1] },
      expenses: { $multiply: ['$expenses', -1] },
    });

    aggregate.addFields({
      bill: { $subtract: ['$expenses', '$payments'] },
    });

    const result = await aggregate.exec();

    return result.length === 1 ? result[0] : null;
  }

  getByBankAccountIdGroupedByDate(
    bankAccountId: string,
    timezone: string,
    args?: { minDate?: string; maxDate?: string },
  ): Promise<{ _id: { month: number; year: number }; balance: number; expenses: number; revenues: number }[]> {
    const aggregate = this.model.aggregate([
      {
        $match: {
          bankAccountId: new Types.ObjectId(bankAccountId),
        },
      },
    ]);

    if (args?.minDate) {
      aggregate.match({
        date: { '$gte': args.minDate },
      });
    }

    if (args?.maxDate) {
      aggregate.match({
        date: { '$lte': args.maxDate },
      });
    }

    aggregate.project({
      date: 1,
      value: 1,
      transactionType: 1,
    });

    aggregate.project({
      date: { '$dateFromString': { dateString: '$date' } },
      value: 1,
      expenses: {
        $cond: [{ $in: ['$transactionType', ['Expense', 'CreditCardBillPayment']] }, '$value', 0],
      },
      revenues: {
        $cond: [
          {
            $eq: ['$transactionType', 'Revenue'],
          },
          '$value',
          0,
        ],
      },
    });

    aggregate.group({
      _id: {
        month: { $month: { date: '$date', timezone } },
        year: { $year: { date: '$date', timezone } },
      },
      balance: { $sum: '$value' },
      expenses: { $sum: '$expenses' },
      revenues: { $sum: '$revenues' },
    });

    aggregate.project({
      _id: '$_id',
      balance: '$balance',
      revenues: '$revenues',
      expenses: { $multiply: ['$expenses', -1] },
    });

    return aggregate.exec();
  }

  getByCreditCardIdGroupedByDate(
    creditCardId: string,
    timezone: string,
    args?: { minCreditCardBillDate?: string; maxCreditCardBillDate?: string },
  ): Promise<{ _id: { month: number; year: number }; expenses: number }[]> {
    const aggregate = this.model.aggregate([
      {
        $match: {
          creditCardId: new Types.ObjectId(creditCardId),
        },
      },
    ]);

    if (args?.minCreditCardBillDate) {
      aggregate.match({
        creditCardBillDate: { '$gte': args.minCreditCardBillDate },
      });
    }

    if (args?.maxCreditCardBillDate) {
      aggregate.match({
        creditCardBillDate: { '$lte': args.maxCreditCardBillDate },
      });
    }

    aggregate.project({
      expenses: {
        $cond: [
          {
            $eq: ['$transactionType', 'CreditCard'],
          },
          '$value',
          0,
        ],
      },
      creditCardBillDate: { '$dateFromString': { dateString: '$creditCardBillDate' } },
    });

    aggregate.group({
      _id: {
        month: { $month: { date: '$creditCardBillDate', timezone } },
        year: { $year: { date: '$creditCardBillDate', timezone } },
      },
      expenses: { $sum: '$expenses' },
    });

    aggregate.project({
      _id: '$_id',
      expenses: { $multiply: ['$expenses', -1] },
    });

    return aggregate.exec();
  }

  getCreditCardBillDates(creditCardId: string): Promise<{ _id: { creditCardBillDate: string } }[]> {
    const aggregate = this.model.aggregate([
      {
        $match: {
          creditCardId: new Types.ObjectId(creditCardId),
        },
      },
      {
        $group: {
          _id: { creditCardBillDate: '$creditCardBillDate' },
        },
      },
    ]);

    return aggregate.exec();
  }
}

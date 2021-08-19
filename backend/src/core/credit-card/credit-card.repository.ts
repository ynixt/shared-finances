import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { MongoDefaultRepository } from '../data';
import { MongoRepositoryOptions } from '../data/mongo-repository';
import { CreditCard, CreditCardDocument } from '../models';
import { EditCreditCardArgs } from '../models/args';

@Injectable()
export class CreditCardRepository extends MongoDefaultRepository<CreditCard, CreditCardDocument> {
  constructor(@InjectModel(CreditCard.name) userDocument: Model<CreditCardDocument>) {
    super(userDocument);
  }

  findByUserId(userId: string, creditCardId: string): Promise<CreditCard | null> {
    return this.model.findOne({ $and: [{ _id: creditCardId }, { userId }] }).exec();
  }

  async edit(userId: string, domain: EditCreditCardArgs): Promise<CreditCard> {
    const result = await this.model
      .findOneAndUpdate(
        { _id: domain.id, userId },
        {
          $set: {
            'closingDay': domain.closingDay,
            'paymentDay': domain.paymentDay,
            'name': domain.name,
            'limit': domain.limit,
            'enabled': domain.enabled,
            'displayOnGroup': domain.displayOnGroup,
          },
        },
        { new: true },
      )
      .exec();

    return result;
  }

  async delete(userId: string, creditCardId: string, opts?: MongoRepositoryOptions): Promise<CreditCard> {
    return this.model.findOneAndDelete({ $and: [{ _id: creditCardId }, { userId }] }, opts).exec();
  }

  existsWithUserId(userId: string, creditCardId: string): Promise<boolean> {
    return this.model.exists({ $and: [{ _id: creditCardId }, { userId }] });
  }

  findAllWithUserId(userId: string): Promise<CreditCard[]> {
    return this.model.find({ userId }).exec();
  }
}

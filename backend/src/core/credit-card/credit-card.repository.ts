import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { MongoEmbededRepository } from '../data';
import { CreditCard, User, UserDocument } from '../models';

@Injectable()
export class CreditCardRepository extends MongoEmbededRepository<CreditCard, User, UserDocument> {
  constructor(@InjectModel(User.name) userDocument: Model<UserDocument>) {
    super(userDocument);
  }

  async create(userId: string, domain: Partial<CreditCard>): Promise<CreditCard> {
    domain = { id: new Types.ObjectId().toHexString(), ...domain };

    const result = await this.model
      .findOneAndUpdate(
        { _id: userId },
        {
          $addToSet: {
            creditCards: domain,
          },
        },
        { new: true },
      )
      .exec();

    return result.creditCards.filter(creditCard => creditCard.id === domain.id)[0];
  }

  async getById(userId: string, creditCardId: string): Promise<CreditCard> {
    const result = await this.model
      .aggregate([
        { $match: { _id: new Types.ObjectId(userId) } },
        {
          $project: {
            creditCards: {
              $filter: {
                input: '$creditCards',
                as: 'creditCards',
                cond: { $eq: ['$$creditCards.id', creditCardId] },
              },
            },
            _id: 0,
          },
        },
        {
          $project: {
            creditCard: {
              $arrayElemAt: ['$creditCards', 0],
            },
          },
        },
      ])
      .exec();

    return result && result.length > 0 ? result[0].creditCard : null;
  }

  async delete(userId: string, creditCardId: string): Promise<boolean> {
    const result = await this.model
      .updateOne(
        { _id: userId },
        {
          $pull: {
            creditCards: { id: creditCardId },
          },
        },
        { new: true },
      )
      .exec();

    return result.n > 0;
  }
}

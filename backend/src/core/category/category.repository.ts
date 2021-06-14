import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { MongoDefaultRepository } from '../data';
import { MongoRepositoryOptions } from '../data/mongo-repository';
import { Category, CategoryDocument } from '../models';

@Injectable()
export class CategoryRepository extends MongoDefaultRepository<Category, CategoryDocument> {
  constructor(@InjectModel(Category.name) userDocument: Model<CategoryDocument>) {
    super(userDocument);
  }

  findAllWithUserId(userId: string): Promise<Category[]> {
    return this.model.find({ userId }).exec();
  }

  findByUserId(userId: string, categoryId: string): Promise<Category | null> {
    return this.model.findOne({ $and: [{ _id: categoryId }, { userId }] }).exec();
  }

  async update(domain: Category): Promise<Category> {
    const result = await this.model
      .findOneAndUpdate(
        { $and: [{ _id: domain.id }, { userId: domain.userId }] },
        {
          $set: {
            'name': domain.name,
            'color': domain.color,
          },
        },
        { new: true },
      )
      .exec();

    return result;
  }

  delete(userId: string, categoryId: string, opts?: MongoRepositoryOptions): Promise<Category | null> {
    return this.model.findOneAndDelete({ $and: [{ _id: categoryId }, { userId }] }, opts).exec();
  }
}

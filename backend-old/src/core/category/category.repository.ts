import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, UpdateQuery } from 'mongoose';
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

  findAllWithGroupId(groupId: string): Promise<Category[]> {
    return this.model.find({ groupId }).exec();
  }

  findByUserId(userId: string, categoryId: string): Promise<Category | null> {
    return this.model.findOne({ $and: [{ _id: categoryId }, { userId }] }).exec();
  }

  findByGroupId(groupId: string, categoryId: string): Promise<Category | null> {
    return this.model.findOne({ $and: [{ _id: categoryId }, { groupId }] }).exec();
  }

  async update(domain: Category): Promise<Category> {
    let updateQuery: UpdateQuery<CategoryDocument>;

    if (domain.userId != null) {
      updateQuery = { userId: domain.userId };
    } else {
      updateQuery = { groupId: domain.groupId };
    }

    const result = await this.model
      .findOneAndUpdate(
        { $and: [{ _id: domain.id }, updateQuery] },
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

  deleteFromGroup(groupId: string, categoryId: string, opts?: MongoRepositoryOptions): Promise<Category | null> {
    return this.model.findOneAndDelete({ $and: [{ _id: categoryId }, { groupId }] }, opts).exec();
  }
}

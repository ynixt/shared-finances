import { Document } from 'mongoose';
import { MongoRepository } from './mongo-repository';

export abstract class MongoDefaultRepository<TDomain, TDocument extends Document & TDomain> extends MongoRepository<TDomain, TDocument> {
  async create(domain: Partial<TDomain>): Promise<TDomain> {
    const createdUser = await this.model.create(domain);

    return this.getById(createdUser.id);
  }

  public async getById(id: string): Promise<TDomain> {
    const document = await this.model.findById(id).exec();

    return document;
  }
}

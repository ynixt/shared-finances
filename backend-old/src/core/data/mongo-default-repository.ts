import { Document } from 'mongoose';
import { MongoRepository, MongoRepositoryOptions } from './mongo-repository';

export abstract class MongoDefaultRepository<TDomain, TDocument extends Document & TDomain> extends MongoRepository<TDomain, TDocument> {
  async create(domain: Partial<TDomain>, opts?: MongoRepositoryOptions): Promise<TDomain> {
    const domainsCreated = await this.model.create([domain], opts);

    if (domainsCreated.length === 1) {
      return this.getById(domainsCreated[0].id, opts);
    }

    return null;
  }

  public async getById(id: string, opts?: MongoRepositoryOptions): Promise<TDomain> {
    const document = await this.model.findById(id, undefined, opts).exec();

    return document;
  }
}

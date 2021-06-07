import { Document, Model } from 'mongoose';
import { MongoRepository } from './mongo-repository';

export abstract class MongoEmbededRepository<TDomain, TParentDomain, TParentDocument extends any & Document> extends MongoRepository<
  TDomain,
  TParentDocument
> {
  constructor(parentModel: Model<TParentDocument>) {
    super(parentModel);
  }
}

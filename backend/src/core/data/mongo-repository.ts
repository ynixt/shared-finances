import { ClientSession } from 'mongoose';
import { Repository } from './repository';
import { Model, Document } from 'mongoose';

export abstract class MongoRepository<TDomain, TDocument extends any & Document> extends Repository<TDomain, ClientSession> {
  constructor(protected model: Model<TDocument>) {
    super();
  }

  openTransaction(): Promise<ClientSession> {
    return this.model.db.startSession();
  }

  abortTransaction(transaction: ClientSession): Promise<void> {
    return transaction.abortTransaction();
  }

  async commitTransaction(transaction: ClientSession): Promise<void> {
    return transaction.endSession();
  }
}

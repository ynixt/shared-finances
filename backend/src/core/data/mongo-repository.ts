import { ClientSession } from 'mongoose';
import { Repository } from './repository';
import { Model } from 'mongoose';

export abstract class MongoRepository<Domain, Document> extends Repository<Domain, ClientSession> {
  constructor(protected model: Model<Document>) {
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

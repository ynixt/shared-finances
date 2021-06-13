import { ClientSession } from 'mongoose';
import { Repository } from './repository';
import { Model, Document } from 'mongoose';

export abstract class MongoRepository<TDomain, TDocument extends any & Document> extends Repository<TDomain, ClientSession> {
  constructor(protected model: Model<TDocument>) {
    super();
  }

  async runInsideTransaction<TReturn>(code: () => Promise<TReturn>): Promise<TReturn>;
  async runInsideTransaction(code: () => void): Promise<void>;

  async runInsideTransaction<TReturn>(code: () => Promise<TReturn> | void): Promise<TReturn | void> {
    const transaction = await this.openTransaction();

    try {
      return code();
    } catch (err) {
      await this.abortTransaction(transaction);
      throw err;
    } finally {
      await this.commitTransaction(transaction);
    }
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

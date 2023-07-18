import { ClientSession } from 'mongoose';
import { Repository } from './repository';
import { Model, Document } from 'mongoose';

export type MongoRepositoryOptions = {
  session: ClientSession;
};

export abstract class MongoRepository<TDomain, TDocument extends any & Document> extends Repository<TDomain, ClientSession> {
  constructor(protected model: Model<TDocument>) {
    super();
  }

  async runInsideTransaction<TReturn>(code: (opts: MongoRepositoryOptions) => Promise<TReturn>): Promise<TReturn>;
  async runInsideTransaction(code: (opts: MongoRepositoryOptions) => void): Promise<void>;

  async runInsideTransaction<TReturn>(code: (opts: MongoRepositoryOptions) => Promise<TReturn> | void): Promise<TReturn | void> {
    const session = await this.openTransaction();

    const opts = { session };

    try {
      const response = await code(opts);
      await this.endTransaction(session);
      await this.endSession(session);
      return response;
    } catch (err) {
      await this.abortTransaction(session);
      await this.endSession(session);
      throw err;
    }
  }

  async openTransaction(): Promise<ClientSession> {
    const session = await this.model.db.startSession();
    session.startTransaction();
    return session;
  }

  abortTransaction(transaction: ClientSession): Promise<void> {
    return transaction.abortTransaction();
  }

  async endTransaction(transaction: ClientSession): Promise<void> {
    return transaction.commitTransaction();
  }

  async endSession(transaction: ClientSession): Promise<void> {
    return transaction.endSession();
  }
}

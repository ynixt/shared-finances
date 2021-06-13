export abstract class Repository<Domain, Transaction> {
  abstract openTransaction(): Promise<Transaction>;
  abstract abortTransaction(transaction: Transaction): Promise<void>;
  abstract endTransaction(transaction: Transaction): Promise<void>;
}

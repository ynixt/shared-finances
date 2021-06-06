export abstract class Repository<Domain, Transaction> {
  abstract openTransaction(): Promise<Transaction>;
  abstract abortTransaction(transaction: Transaction): Promise<void>;
  abstract commitTransaction(transaction: Transaction): Promise<void>;
}

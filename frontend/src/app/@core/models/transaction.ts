import { TransactionType } from '../enums';

export interface Transaction {
  id: string;

  transactionType: TransactionType;

  date: string;

  value: number;

  description?: string;

  bankAccountId: string;
}

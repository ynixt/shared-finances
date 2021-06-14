import { TransactionType } from '../enums';
import { Category } from './category';

export interface Transaction {
  id: string;
  transactionType: TransactionType;
  date: string;
  value: number;
  description?: string;
  bankAccountId?: string;
  bankAccount2Id?: string;
  creditCardId?: string;
  categoryId?: string;
  category?: Category;
}

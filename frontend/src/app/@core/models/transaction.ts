import { TransactionType } from '../enums';
import { Category } from './category';

export interface Transaction {
  id: string;
  transactionType: TransactionType;
  date: string;
  value: number;
  description?: string;
  bankAccountId: string;
  categoryId?: string;
  category?: Category;
}

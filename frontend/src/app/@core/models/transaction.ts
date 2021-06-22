import { TransactionType } from '../enums';
import { Category } from './category';
import { User } from './user';

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
  groupId?: string;
  user: Partial<User>;
  user2?: Partial<User>;
}

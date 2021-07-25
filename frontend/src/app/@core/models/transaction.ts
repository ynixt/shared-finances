import { TransactionType } from '../enums';
import { Category } from './category';
import { Group } from './group';
import { User } from './user';

export interface Transaction {
  id: string;
  transactionType: TransactionType;
  date: string;
  creditCardBillDate?: string;
  value: number;
  description?: string;
  bankAccountId?: string;
  bankAccount2Id?: string;
  creditCardId?: string;
  categoryId?: string;
  category?: Category;
  group?: Group;
  groupId?: string;
  user: Partial<User>;
  user2?: Partial<User>;

  installmentId?: string;
  installment?: number;
  totalInstallments?: number;
}

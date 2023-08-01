import { TransactionType } from "../enums";
import { Category } from "./category";
import { Group } from "./group";
import { User } from "./user";
import { BankAccount } from "./bank-account";
import { CreditCard } from "./credit-card";

export interface Transaction {
  id: string;
  type: TransactionType;
  date: string;
  creditCardBillDateValue?: string;
  value: number;
  description?: string;
  bankAccountId?: string;
  bankAccount?: BankAccount;
  bankAccount2Id?: string;
  creditCardId?: string;
  creditCard?: CreditCard;
  categoryId?: string;
  category?: Category;
  group?: Group;
  groupId?: string;
  user: Partial<User>;
  user2?: Partial<User>;
  otherSide?: {
    id: string;
    userId: string;
    bankAccount?: BankAccount
  };
  installmentId?: string;
  installment?: number;
  totalInstallments?: number;
}

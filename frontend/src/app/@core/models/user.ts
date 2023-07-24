import { BankAccount } from './bank-account';
import { CreditCard } from './credit-card';

export interface User {
  id: string;
  uid: string;
  email: string;
  name: string;
  photoUrl: string;
  permissions?: any[];
  creditCards?: CreditCard[];
  bankAccounts?: BankAccount[];
}

import { CreditCard } from './credit-card';

export interface User {
  id: string;
  uid: string;
  email: string;
  name: string;
  photoURL: string;
  permissions?: any[];
  creditCards?: CreditCard[];
}

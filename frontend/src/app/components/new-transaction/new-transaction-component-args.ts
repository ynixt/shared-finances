import { Transaction } from 'src/app/@core/models';

export interface NewTransactionComponentArgs {
  shared: boolean;
  transaction?: Transaction;
}

import { Moment } from 'moment';
import { CreditCard, Transaction } from 'src/app/@core/models';

export interface CreditCardBillPaymentComponentArgs {
  creditCard: CreditCard;
  creditCardBillDate: string | Moment;
  transaction?: Transaction;
  billValue?: number;
}

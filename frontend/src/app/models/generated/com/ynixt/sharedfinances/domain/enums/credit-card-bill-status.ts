/* eslint-disable */
/* tslint-disable */

export type CreditCardBillStatus = 'OPEN' | 'PAID' | 'OVERDUE' | 'FUTURE' | 'CLOSED';

export const CreditCardBillStatus__Options: CreditCardBillStatus[] = ['OPEN', 'PAID', 'OVERDUE', 'FUTURE', 'CLOSED'];

export const CreditCardBillStatus__Obj: { [K in CreditCardBillStatus]: CreditCardBillStatus } = {
  'OPEN': 'OPEN',
  'PAID': 'PAID',
  'OVERDUE': 'OVERDUE',
  'FUTURE': 'FUTURE',
  'CLOSED': 'CLOSED',
};

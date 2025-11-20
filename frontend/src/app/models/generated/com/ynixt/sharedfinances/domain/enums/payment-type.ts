/* eslint-disable */
/* tslint-disable */

export type PaymentType = 'UNIQUE' | 'RECURRING' | 'INSTALLMENTS';

export const PaymentType__Options: PaymentType[] = ['UNIQUE', 'RECURRING', 'INSTALLMENTS'];

export const PaymentType__Obj: { [K in PaymentType]: PaymentType } = {
  'UNIQUE': 'UNIQUE',
  'RECURRING': 'RECURRING',
  'INSTALLMENTS': 'INSTALLMENTS',
};

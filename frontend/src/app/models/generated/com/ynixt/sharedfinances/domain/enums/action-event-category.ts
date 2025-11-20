/* eslint-disable */
/* tslint-disable */

export type ActionEventCategory =
  | 'BANK_ACCOUNT'
  | 'GROUP'
  | 'CREDIT_CARD'
  | 'BANK_ACCOUNT_ASSOCIATE'
  | 'CREDIT_CARD_ASSOCIATE'
  | 'USER_CATEGORY'
  | 'GROUP_CATEGORY'
  | 'WALLET_ENTRY';

export const ActionEventCategory__Options: ActionEventCategory[] = [
  'BANK_ACCOUNT',
  'GROUP',
  'CREDIT_CARD',
  'BANK_ACCOUNT_ASSOCIATE',
  'CREDIT_CARD_ASSOCIATE',
  'USER_CATEGORY',
  'GROUP_CATEGORY',
  'WALLET_ENTRY',
];

export const ActionEventCategory__Obj: { [K in ActionEventCategory]: ActionEventCategory } = {
  'BANK_ACCOUNT': 'BANK_ACCOUNT',
  'GROUP': 'GROUP',
  'CREDIT_CARD': 'CREDIT_CARD',
  'BANK_ACCOUNT_ASSOCIATE': 'BANK_ACCOUNT_ASSOCIATE',
  'CREDIT_CARD_ASSOCIATE': 'CREDIT_CARD_ASSOCIATE',
  'USER_CATEGORY': 'USER_CATEGORY',
  'GROUP_CATEGORY': 'GROUP_CATEGORY',
  'WALLET_ENTRY': 'WALLET_ENTRY',
};

/* eslint-disable */
/* tslint-disable */

export interface CreditCardBillDto {
  billDate?: string | null;
  closingDate?: string | null;
  creditCardId: string;
  dueDate?: string | null;
  id?: string | null;
  payed: boolean;
  value: number;
}

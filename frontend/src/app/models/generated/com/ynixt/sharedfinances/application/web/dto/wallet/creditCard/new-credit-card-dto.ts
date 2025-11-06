/* eslint-disable */
/* tslint-disable */

export interface NewCreditCardDto {
  currency: string;
  daysBetweenDueAndClosing: number;
  dueDay: number;
  dueOnNextBusinessDay: boolean;
  name: string;
  totalLimit: number;
}

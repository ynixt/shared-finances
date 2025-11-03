/* tslint-disable */

export interface EditCreditCardDto {
  newCurrency: string;
  newDaysBetweenDueAndClosing: number;
  newDueDay: number;
  newDueOnNextBusinessDay: boolean;
  newEnabled: boolean;
  newName: string;
  newTotalLimit: number;
}

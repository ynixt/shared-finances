/* eslint-disable */
/* tslint-disable */

export interface CreateGroupDebtAdjustmentRequestDto {
  amountDelta: number;
  currency: string;
  month: string;
  note?: string | null;
  payerId: string;
  receiverId: string;
}

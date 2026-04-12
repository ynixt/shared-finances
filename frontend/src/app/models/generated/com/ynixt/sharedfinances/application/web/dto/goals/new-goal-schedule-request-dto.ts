/* eslint-disable */
/* tslint-disable */
import { RecurrenceType } from '../../../../domain/enums/recurrence-type';

export interface NewGoalScheduleRequestDto {
  amount: number;
  firstExecution: string;
  periodicity: RecurrenceType;
  qtyLimit?: number | null;
  removesAllocation: boolean;
  walletItemId: string;
}

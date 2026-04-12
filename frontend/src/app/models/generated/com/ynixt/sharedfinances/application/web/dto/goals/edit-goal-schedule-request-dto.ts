/* eslint-disable */
/* tslint-disable */
import { RecurrenceType } from '../../../../domain/enums/recurrence-type';

export interface EditGoalScheduleRequestDto {
  amount: number;
  nextExecution: string;
  periodicity: RecurrenceType;
  qtyLimit?: number | null;
  removesAllocation: boolean;
  walletItemId: string;
}

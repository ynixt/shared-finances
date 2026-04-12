/* eslint-disable */
/* tslint-disable */
import { RecurrenceType } from '../../../../domain/enums/recurrence-type';

export interface GoalContributionScheduleDto {
  amount: number;
  currency: string;
  endExecution?: string | null;
  id: string;
  lastExecution?: string | null;
  nextExecution?: string | null;
  periodicity: RecurrenceType;
  qtyExecuted: number;
  qtyLimit?: number | null;
  removesAllocation: boolean;
  walletItemId: string;
  walletItemName: string;
}

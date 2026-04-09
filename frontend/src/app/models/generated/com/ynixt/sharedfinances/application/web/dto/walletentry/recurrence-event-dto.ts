/* eslint-disable */
/* tslint-disable */
import { PaymentType } from '../../../../domain/enums/payment-type';
import { RecurrenceType } from '../../../../domain/enums/recurrence-type';

export interface RecurrenceEventDto {
  endExecution?: string | null;
  id: string;
  lastExecution?: string | null;
  nextExecution?: string | null;
  paymentType: PaymentType;
  periodicity: RecurrenceType;
  qtyExecuted: number;
  qtyLimit?: number | null;
}

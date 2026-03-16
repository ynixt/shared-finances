/* eslint-disable */
/* tslint-disable */

export interface RecurrenceEventDto {
  endExecution?: string | null;
  id: string;
  lastExecution?: string | null;
  nextExecution?: string | null;
  qtyExecuted: number;
  qtyLimit?: number | null;
}

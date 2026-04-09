/* eslint-disable */
/* tslint-disable */
import { ScheduledExecutionFilter } from '../../../../domain/enums/scheduled-execution-filter';

export interface ScheduledExecutionManagerRequestDto {
  filter?: ScheduledExecutionFilter | null;
  groupId?: string | null;
}

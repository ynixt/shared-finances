/* eslint-disable */
/* tslint-disable */
import { ScheduledEditScope } from '../../../../domain/enums/scheduled-edit-scope';

export interface DeleteScheduledEntryDto {
  occurrenceDate: string;
  scope?: ScheduledEditScope | null;
}

/* eslint-disable */
/* tslint-disable */
import { ScheduledEditScope } from '../../../../domain/enums/scheduled-edit-scope';
import { NewEntryDto } from './new-entry-dto';

export interface EditScheduledEntryDto {
  entry: NewEntryDto;
  occurrenceDate: string;
  scope: ScheduledEditScope;
}

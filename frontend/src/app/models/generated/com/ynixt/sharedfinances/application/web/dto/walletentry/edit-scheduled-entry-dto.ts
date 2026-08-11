/* eslint-disable */
/* tslint-disable */

import { NewEntryDto } from './new-entry-dto';
import { ScheduledEditScope } from '../../../../domain/enums/scheduled-edit-scope';

export interface EditScheduledEntryDto {
  entry: NewEntryDto;
  occurrenceDate: string;
  scope: ScheduledEditScope;
}

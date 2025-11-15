/* eslint-disable */
/* tslint-disable */

import { ActionEventType } from '../../../../domain/enums/action-event-type';

export interface UserActionEventDto {
  data: any;
  groupId?: string | null;
  id: string;
  modifiedByUserId?: string | null;
  type: ActionEventType;
}

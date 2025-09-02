/* eslint-disable */
/* tslint-disable */
import { ActionEventType } from '../../../../domain/enums/action-event-type';

export interface UserActionEventDto {
  data: any;
  id: string;
  type: ActionEventType;
}

/* eslint-disable */
/* tslint-disable */
import { WalletEntryType } from '../../../../domain/enums/wallet-entry-type';
import { GroupDto } from '../groups/group-dto';
import { UserSimpleDto } from '../user/user-simple-dto';
import { CategoryDto } from '../wallet/category/category-dto';
import { EntryResponseDto } from './EventForListDto/entry-response-dto';
import { RecurrenceEventDto } from './recurrence-event-dto';

export interface EventForListDto {
  category?: CategoryDto | null;
  confirmed: boolean;
  currency: string;
  date: string;
  entries: Array<EntryResponseDto>;
  group?: GroupDto | null;
  id?: string | null;
  installment?: number | null;
  name?: string | null;
  observations?: string | null;
  recurrenceConfig?: RecurrenceEventDto | null;
  recurrenceConfigId?: string | null;
  tags?: Array<string> | null;
  type: WalletEntryType;
  user?: UserSimpleDto | null;
}

/* eslint-disable */
/* tslint-disable */
import { WalletEntryType } from '../../../../domain/enums/wallet-entry-type';
import { GroupDto } from '../groups/group-dto';
import { UserSimpleDto } from '../user/user-simple-dto';
import { CategoryDto } from '../wallet/category/category-dto';
import { WalletItemForEntryListDto } from '../wallet/wallet-item-for-entry-list-dto';

export interface EntryForListDto {
  category?: CategoryDto | null;
  confirmed: boolean;
  date: string;
  group?: GroupDto | null;
  id: string;
  installment?: number | null;
  name?: string | null;
  observations?: string | null;
  origin: WalletItemForEntryListDto;
  recurrenceConfigId?: string | null;
  tags?: Array<string> | null;
  target?: WalletItemForEntryListDto | null;
  type: WalletEntryType;
  user?: UserSimpleDto | null;
  value: number;
}

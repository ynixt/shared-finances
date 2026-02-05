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
  currency: string;
  date: string;
  group?: GroupDto | null;
  id?: string | null;
  installment?: number | null;
  name?: string | null;
  observations?: string | null;
  origin: WalletItemForEntryListDto;
  originBillDate?: string | null;
  originBillId?: string | null;
  recurrenceConfigId?: string | null;
  tags?: Array<string> | null;
  target?: WalletItemForEntryListDto | null;
  targetBillDate?: string | null;
  targetBillId?: string | null;
  type: WalletEntryType;
  user?: UserSimpleDto | null;
  value: number;
}

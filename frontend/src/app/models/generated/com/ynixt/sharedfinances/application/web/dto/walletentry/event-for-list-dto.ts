/* eslint-disable */
/* tslint-disable */
import { TransferPurpose } from '../../../../domain/enums/transfer-purpose';
import { WalletEntryType } from '../../../../domain/enums/wallet-entry-type';
import { GroupDto } from '../groups/group-dto';
import { UserSimpleDto } from '../user/user-simple-dto';
import { CategoryDto } from '../wallet/category/category-dto';
import { EntryResponseDto } from './EventForListDto/entry-response-dto';
import { RecurrenceEventDto } from './recurrence-event-dto';
import { WalletBeneficiaryLegDto } from './wallet-beneficiary-leg-dto';

export interface EventForListDto {
  beneficiaries: Array<WalletBeneficiaryLegDto>;
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
  originValue?: number | null;
  recurrenceConfig?: RecurrenceEventDto | null;
  recurrenceConfigId?: string | null;
  tags?: Array<string> | null;
  targetValue?: number | null;
  transferPurpose?: TransferPurpose | null;
  type: WalletEntryType;
  user?: UserSimpleDto | null;
}

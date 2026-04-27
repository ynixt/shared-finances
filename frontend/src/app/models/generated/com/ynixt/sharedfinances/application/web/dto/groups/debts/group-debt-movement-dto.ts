/* eslint-disable */
/* tslint-disable */
import { GroupDebtMovementReasonKind } from '../../../../../domain/enums/group-debt-movement-reason-kind';
import { EventForListDto } from '../../walletentry/event-for-list-dto';

export interface GroupDebtMovementDto {
  createdAt?: any | null;
  createdByUserId: string;
  currency: string;
  deltaSigned: number;
  id: string;
  month: string;
  note?: string | null;
  payerId: string;
  reasonKind: GroupDebtMovementReasonKind;
  receiverId: string;
  sourceMovementId?: string | null;
  sourceWalletEvent?: EventForListDto | null;
  sourceWalletEventId?: string | null;
}

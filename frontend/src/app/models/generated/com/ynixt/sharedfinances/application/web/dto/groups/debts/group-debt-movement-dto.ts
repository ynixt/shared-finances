/* eslint-disable */
/* tslint-disable */
import { GroupDebtMovementReasonKind } from '../../../../../domain/enums/group-debt-movement-reason-kind';

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
  sourceWalletEventId?: string | null;
}

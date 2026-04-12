/* eslint-disable */
/* tslint-disable */
import { GoalLedgerMovementKind } from '../../../../domain/enums/goal-ledger-movement-kind';

export interface GoalLedgerMovementDto {
  createdAt?: any | null;
  currency: string;
  id: string;
  movementDate: string;
  movementKind: GoalLedgerMovementKind;
  note?: string | null;
  scheduleId?: string | null;
  signedAmount: number;
  walletItemId: string;
  walletItemName: string;
}

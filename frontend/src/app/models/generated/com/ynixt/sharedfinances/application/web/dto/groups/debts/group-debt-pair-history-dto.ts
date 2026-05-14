/* eslint-disable */
/* tslint-disable */
import { GroupDebtMovementDto } from './group-debt-movement-dto';

export interface GroupDebtPairHistoryDto {
  chargeDelta: number;
  currency: string;
  firstUserId: string;
  lines: Array<GroupDebtMovementDto>;
  manualAdjustmentDelta: number;
  month: string;
  netAmount: number;
  netPayerId?: string | null;
  netReceiverId?: string | null;
  secondUserId: string;
  settlementDelta: number;
}

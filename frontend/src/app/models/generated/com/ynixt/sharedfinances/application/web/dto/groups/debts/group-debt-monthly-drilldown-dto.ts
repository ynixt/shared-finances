/* eslint-disable */
/* tslint-disable */
import { GroupDebtMovementDto } from './group-debt-movement-dto';

export interface GroupDebtMonthlyDrilldownDto {
  chargeDelta: number;
  currency: string;
  lines: Array<GroupDebtMovementDto>;
  manualAdjustmentDelta: number;
  month: string;
  netAmount: number;
  payerId: string;
  receiverId: string;
  settlementDelta: number;
}

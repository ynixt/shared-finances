/* eslint-disable */
/* tslint-disable */
import { GroupDebtMonthlyCompositionDto } from './group-debt-monthly-composition-dto';

export interface GroupDebtPairBalanceDto {
  currency: string;
  monthlyComposition: Array<GroupDebtMonthlyCompositionDto>;
  outstandingAmount: number;
  payerId: string;
  receiverId: string;
}

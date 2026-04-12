/* eslint-disable */
/* tslint-disable */
import { FinancialGoalTargetDto } from './financial-goal-target-dto';

export interface EditFinancialGoalRequestDto {
  deadline?: string | null;
  description?: string | null;
  name: string;
  targets: Array<FinancialGoalTargetDto>;
}

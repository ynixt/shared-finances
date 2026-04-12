/* eslint-disable */
/* tslint-disable */
import { FinancialGoalSummaryDto } from './financial-goal-summary-dto';
import { FinancialGoalTargetDto } from './financial-goal-target-dto';
import { GoalCommitmentChartSeriesDto } from './goal-commitment-chart-series-dto';

export interface FinancialGoalDetailDto {
  commitmentChart: Array<GoalCommitmentChartSeriesDto>;
  committedByCurrency: { [key: string]: number };
  goal: FinancialGoalSummaryDto;
  targets: Array<FinancialGoalTargetDto>;
}

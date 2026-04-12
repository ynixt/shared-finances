/* eslint-disable */
/* tslint-disable */
import { GoalCommitmentMonthlyPointDto } from './goal-commitment-monthly-point-dto';

export interface GoalCommitmentChartSeriesDto {
  currency: string;
  points: Array<GoalCommitmentMonthlyPointDto>;
  targetAmount: number;
}

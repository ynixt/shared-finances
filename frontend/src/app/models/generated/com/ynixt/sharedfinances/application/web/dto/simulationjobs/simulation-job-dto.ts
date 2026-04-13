/* eslint-disable */
/* tslint-disable */
import { SimulationJobStatus } from '../../../../domain/enums/simulation-job-status';
import { SimulationJobType } from '../../../../domain/enums/simulation-job-type';

export interface SimulationJobDto {
  cancelledAt?: any | null;
  createdAt?: any | null;
  errorMessage?: string | null;
  finishedAt?: any | null;
  id: string;
  requestedByUserId: string;
  requestPayload?: string | null;
  resultPayload?: string | null;
  retries: number;
  startedAt?: any | null;
  status: SimulationJobStatus;
  type: SimulationJobType;
  updatedAt?: any | null;
}

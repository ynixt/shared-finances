/* eslint-disable */
/* tslint-disable */
import { SimulationJobStatus } from '../../../../domain/enums/simulation-job-status';
import { SimulationJobType } from '../../../../domain/enums/simulation-job-type';

export interface SimulationJobStatusEventDto {
  cancelledAt?: any | null;
  errorMessage?: string | null;
  finishedAt?: any | null;
  id: string;
  resultPayload?: string | null;
  retries: number;
  status: SimulationJobStatus;
  type: SimulationJobType;
}

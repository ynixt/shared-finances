/* eslint-disable */
/* tslint-disable */
import { SimulationJobType } from '../../../../domain/enums/simulation-job-type';

export interface CreateSimulationJobRequestDto {
  requestPayload?: string | null;
  type: SimulationJobType;
}

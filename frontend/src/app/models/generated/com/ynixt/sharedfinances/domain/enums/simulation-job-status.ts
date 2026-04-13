/* eslint-disable */
/* tslint-disable */

export type SimulationJobStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export const SimulationJobStatus__Options: SimulationJobStatus[] = ['QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'];

export const SimulationJobStatus__Obj: { [K in SimulationJobStatus]: SimulationJobStatus } = {
  'QUEUED': 'QUEUED',
  'RUNNING': 'RUNNING',
  'COMPLETED': 'COMPLETED',
  'FAILED': 'FAILED',
  'CANCELLED': 'CANCELLED',
};

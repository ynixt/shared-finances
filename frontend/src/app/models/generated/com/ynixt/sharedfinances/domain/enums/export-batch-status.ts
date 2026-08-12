/* eslint-disable */
/* tslint-disable */

export type ExportBatchStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'EXPIRED';

export const ExportBatchStatus__Options: ExportBatchStatus[] = ['QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'EXPIRED'];

export const ExportBatchStatus__Obj: { [K in ExportBatchStatus]: ExportBatchStatus } = {
  'QUEUED': 'QUEUED',
  'RUNNING': 'RUNNING',
  'COMPLETED': 'COMPLETED',
  'FAILED': 'FAILED',
  'EXPIRED': 'EXPIRED',
};

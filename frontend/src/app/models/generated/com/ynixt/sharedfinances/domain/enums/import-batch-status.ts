/* eslint-disable */
/* tslint-disable */

export type ImportBatchStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'UNDO_QUEUED' | 'UNDO_RUNNING' | 'UNDO_FAILED';

export const ImportBatchStatus__Options: ImportBatchStatus[] = [
  'QUEUED',
  'RUNNING',
  'COMPLETED',
  'FAILED',
  'UNDO_QUEUED',
  'UNDO_RUNNING',
  'UNDO_FAILED',
];

export const ImportBatchStatus__Obj: { [K in ImportBatchStatus]: ImportBatchStatus } = {
  'QUEUED': 'QUEUED',
  'RUNNING': 'RUNNING',
  'COMPLETED': 'COMPLETED',
  'FAILED': 'FAILED',
  'UNDO_QUEUED': 'UNDO_QUEUED',
  'UNDO_RUNNING': 'UNDO_RUNNING',
  'UNDO_FAILED': 'UNDO_FAILED',
};

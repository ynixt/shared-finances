import { ImportBatchDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/imports';
import { ImportBatchStatus } from '../../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';

export function importBatchStatusLabelKey(status: ImportBatchStatus): string {
  switch (status) {
    case 'QUEUED':
    case 'RUNNING':
      return 'financesPage.transactionsPage.importPage.status.inProgress';
    case 'COMPLETED':
      return 'financesPage.transactionsPage.importPage.status.imported';
    case 'FAILED':
      return 'financesPage.transactionsPage.importPage.status.failed';
    case 'UNDO_QUEUED':
    case 'UNDO_RUNNING':
      return 'financesPage.transactionsPage.importPage.status.undoing';
    case 'UNDO_FAILED':
      return 'financesPage.transactionsPage.importPage.status.undoFailed';
  }
}

export function canUndoImportBatch(batch: Pick<ImportBatchDto, 'status'>): boolean {
  return batch.status === 'COMPLETED' || batch.status === 'UNDO_FAILED';
}

export function hasCompletedImportBatch(batch: Pick<ImportBatchDto, 'status'>): boolean {
  return batch.status === 'COMPLETED' || batch.status.startsWith('UNDO_');
}

export function isTerminalImportBatchStatus(status: ImportBatchStatus): boolean {
  return status === 'COMPLETED' || status === 'FAILED' || status === 'UNDO_FAILED';
}

export function isStaleImportBatchLifecycleRegression(existing: ImportBatchStatus, incoming: ImportBatchStatus): boolean {
  if (existing.startsWith('UNDO_') && !incoming.startsWith('UNDO_')) return true;
  return (existing === 'COMPLETED' || existing === 'FAILED') && (incoming === 'QUEUED' || incoming === 'RUNNING');
}

export function mergeImportBatch(history: readonly ImportBatchDto[], batch: ImportBatchDto): ImportBatchDto[] {
  const existing = history.find(item => item.id === batch.id);
  if (existing && isStaleImportBatchLifecycleRegression(existing.status, batch.status)) return [...history];
  return [batch, ...history.filter(item => item.id !== batch.id)];
}

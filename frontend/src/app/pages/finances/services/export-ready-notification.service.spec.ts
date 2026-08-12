import '@angular/compiler';

import { Subject } from 'rxjs';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ExportReadyNotificationService } from './export-ready-notification.service';

describe('ExportReadyNotificationService', () => {
  let exportEvents: Subject<unknown>;

  beforeEach(() => {
    exportEvents = new Subject();
  });

  it('notifies every tab once for duplicate completion delivery', () => {
    const firstMessages = { add: vi.fn() };
    const secondMessages = { add: vi.fn() };
    const events = { exportBatchAction$: exportEvents };
    const translate = { instant: (key: string) => key };
    const firstTab = new ExportReadyNotificationService(events as never, firstMessages as never, translate as never);
    const secondTab = new ExportReadyNotificationService(events as never, secondMessages as never, translate as never);
    const completed = { id: 'export-1', status: 'COMPLETED' };

    exportEvents.next({ type: 'UPDATE', data: completed });
    exportEvents.next({ type: 'UPDATE', data: completed });

    const notification = {
      severity: 'success',
      summary: 'financesPage.transactionsPage.exportPage.notifications.ready',
    };
    expect(firstMessages.add).toHaveBeenCalledTimes(1);
    expect(firstMessages.add).toHaveBeenCalledWith(notification);
    expect(secondMessages.add).toHaveBeenCalledTimes(1);
    expect(secondMessages.add).toHaveBeenCalledWith(notification);
    firstTab.ngOnDestroy();
    secondTab.ngOnDestroy();
  });
});

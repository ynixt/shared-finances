import { Injectable, OnDestroy } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { Subscription } from 'rxjs';

import { MessageService } from 'primeng/api';

import { ExportBatchStatusEventDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/exports';
import { UserActionEventService } from './user-action-event.service';

@Injectable()
export class ExportReadyNotificationService implements OnDestroy {
  private readonly notifiedBatchIds = new Set<string>();
  private readonly subscription: Subscription;

  constructor(
    events: UserActionEventService,
    private readonly messages: MessageService,
    private readonly translate: TranslateService,
  ) {
    this.subscription = events.exportBatchAction$.subscribe(event => {
      if (event.type === 'DELETE') {
        this.notifiedBatchIds.delete(event.data as string);
        return;
      }
      const batch = event.data as ExportBatchStatusEventDto;
      if (batch.status !== 'COMPLETED' || this.notifiedBatchIds.has(batch.id)) return;
      this.notifiedBatchIds.add(batch.id);
      this.messages.add({
        severity: 'success',
        summary: this.translate.instant('financesPage.transactionsPage.exportPage.notifications.ready'),
      });
    });
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }
}

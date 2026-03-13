import { Component, computed, input } from '@angular/core';
import { FaIconComponent, IconDefinition } from '@fortawesome/angular-fontawesome';
import { faCheck, faClock, faXmark } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe } from '@ngx-translate/core';

import dayjs from 'dayjs';

import { EventForListDto } from '../../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';

enum Status {
  PENDING,
  CONFIRMED,
  FUTURE,
}

@Component({
  selector: 'app-entry-status',
  imports: [FaIconComponent, TranslatePipe],
  templateUrl: './entry-status.component.html',
  styleUrl: './entry-status.component.scss',
  standalone: true,
})
export class EntryStatusComponent {
  readonly entry = input<EventForListDto | undefined>(undefined);

  status = computed<Status | undefined>(() => {
    const entry = this.entry();

    if (entry == null) return undefined;

    if (this.transactionIsInFuture(entry)) return Status.FUTURE;
    if (entry.confirmed) return Status.CONFIRMED;

    return Status.PENDING;
  });

  statusFaIcon = computed<IconDefinition | undefined>(() => {
    switch (this.status()) {
      case Status.PENDING:
        return faXmark;
      case Status.CONFIRMED:
        return faCheck;
      case Status.FUTURE:
        return faClock;
    }

    return undefined;
  });

  statusClassColor = computed<string>(() => {
    switch (this.status()) {
      case Status.PENDING:
        return 'text-yellow-500';
      case Status.CONFIRMED:
        return 'text-green-500';
      case Status.FUTURE:
        return 'text-blue-500';
    }

    return '';
  });

  statusText = computed<string | undefined>(() => {
    switch (this.status()) {
      case Status.PENDING:
        return 'financesPage.transactionsPage.status.pending';
      case Status.CONFIRMED:
        return 'financesPage.transactionsPage.status.confirmed';
      case Status.FUTURE:
        return 'financesPage.transactionsPage.status.future';
    }

    return undefined;
  });

  private transactionIsInFuture(entry: EventForListDto): boolean {
    return dayjs(entry.date, 'YYYY-MM-DD').isAfter(dayjs());
  }
}

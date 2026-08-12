import { DatePipe } from '@angular/common';
import { Component, OnDestroy, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonDirective } from 'primeng/button';
import { TableModule } from 'primeng/table';

import { ExportBatchDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/exports';

@Component({
  selector: 'app-export-history',
  imports: [DatePipe, TranslatePipe, ButtonDirective, TableModule],
  templateUrl: './export-history.component.html',
})
export class ExportHistoryComponent implements OnDestroy {
  readonly batches = input.required<ExportBatchDto[]>();
  readonly downloaded = output<ExportBatchDto>();
  readonly deleted = output<ExportBatchDto>();
  now = Date.now();
  private readonly timer = window.setInterval(() => (this.now = Date.now()), 1_000);

  get visibleBatches(): ExportBatchDto[] {
    return this.batches().filter(batch => batch.status !== 'EXPIRED');
  }

  remaining(batch: ExportBatchDto): string | null {
    if (batch.downloadExpiresAt == null || !batch.downloadAvailable) return null;
    const seconds = Math.max(0, Math.ceil((new Date(batch.downloadExpiresAt).getTime() - this.now) / 1_000));
    return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
  }

  ngOnDestroy(): void {
    window.clearInterval(this.timer);
  }
}

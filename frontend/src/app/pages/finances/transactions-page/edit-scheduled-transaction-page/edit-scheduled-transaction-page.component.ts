import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { ProgressSpinner } from 'primeng/progressspinner';

import {
  EditScheduledEntryDto,
  EventForListDto,
  NewEntryDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { ScheduledEditScope__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { ONLY_DATE_FORMAT } from '../../../../util/date-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { WalletEntryService } from '../../services/wallet-entry.service';
import { TransactionFormComponent } from '../shared/transaction-form/transaction-form.component';

@Component({
  selector: 'app-edit-scheduled-transaction-page',
  imports: [TranslatePipe, FinancesTitleBarComponent, TransactionFormComponent, ProgressSpinner],
  templateUrl: './edit-scheduled-transaction-page.component.html',
})
@UntilDestroy()
export class EditScheduledTransactionPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly walletEntryService = inject(WalletEntryService);
  private readonly messageService = inject(MessageService);
  private readonly errorMessageService = inject(ErrorMessageService);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly entry = signal<EventForListDto | null>(null);
  readonly withFuture = signal(false);
  readonly isRecurring = computed(() => this.entry()?.recurrenceConfigId != null);

  constructor() {
    this.route.paramMap.pipe(untilDestroyed(this)).subscribe(() => {
      this.syncScopeFromQuery();
      void this.loadEntry();
    });

    this.route.queryParamMap.pipe(untilDestroyed(this)).subscribe(() => {
      this.syncScopeFromQuery();
    });
  }

  async submit(request: NewEntryDto) {
    const currentEntry = this.entry();
    if (currentEntry == null || currentEntry.recurrenceConfigId == null || this.submitting()) {
      return;
    }

    this.submitting.set(true);

    try {
      const scope = this.withFuture() ? ScheduledEditScope__Obj.THIS_AND_FUTURE : ScheduledEditScope__Obj.ONLY_THIS;

      const scheduledRequest: EditScheduledEntryDto = {
        occurrenceDate: dayjs(currentEntry.date).format(ONLY_DATE_FORMAT),
        scope: scope,
        entry: request,
      };

      await this.walletEntryService.editScheduledEntry(currentEntry.recurrenceConfigId, scheduledRequest);
      await this.router.navigate(['../..'], { relativeTo: this.route });
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
      throw error;
    } finally {
      this.submitting.set(false);
    }
  }

  private syncScopeFromQuery() {
    this.withFuture.set(this.route.snapshot.queryParamMap.get('withFuture') === 'true');
  }

  private async loadEntry() {
    const recurrenceConfigId = this.route.snapshot.paramMap.get('recurrenceConfigId');
    if (recurrenceConfigId == null) {
      await this.router.navigateByUrl('/not-found');
      return;
    }

    this.loading.set(true);

    try {
      this.entry.set(await this.walletEntryService.getScheduledEntryByRecurrenceConfigId(recurrenceConfigId));
    } catch (error) {
      if (error instanceof HttpErrorResponse && (error.status === 404 || error.status === 400)) {
        await this.router.navigateByUrl('/not-found');
        return;
      }

      this.errorMessageService.handleError(error, this.messageService);
      throw error;
    } finally {
      this.loading.set(false);
    }
  }
}

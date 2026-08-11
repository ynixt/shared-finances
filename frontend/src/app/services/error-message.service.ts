import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';

import { AppResponseErrorDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto';
import { DEFAULT_ERROR_LIFE } from '../util/error-util';

@Injectable({ providedIn: 'root' })
export class ErrorMessageService {
  constructor(private translateService: TranslateService) {}

  handleError(error: any, messageService: MessageService) {
    console.log(error);
    if (error instanceof HttpErrorResponse) {
      if (error.error instanceof Array) {
        error.error.forEach(e => this.addApiErrorOnMessageService(messageService, e));
      } else {
        this.addApiErrorOnMessageService(messageService, error.error);
      }
    } else {
      messageService.add({
        severity: 'error',
        summary: this.translateService.instant('error.genericTitle'),
        detail: this.translateService.instant('error.genericMessage'),
        life: DEFAULT_ERROR_LIFE,
      });
    }
  }

  private addApiErrorOnMessageService(messageService: MessageService, apiError: AppResponseErrorDto | undefined | null) {
    const isPlanQuota = apiError?.errorCode === 'PLAN_QUOTA_EXCEEDED';
    const quota = apiError?.argsI18n?.['quota'];
    const quotaName = quota == null ? '' : this.translateService.instant(`planQuota.quotas.${quota}`);
    const detail = isPlanQuota
      ? this.translateService.instant(
          apiError?.argsI18n?.['groupId'] != null
            ? 'planQuota.refusal.group'
            : apiError?.argsI18n?.['quotaOwnerUserId'] == null
              ? 'planQuota.refusal.own'
              : 'planQuota.refusal.other',
          { quota: quotaName },
        )
      : this.translateService.instant(apiError?.messageI18n ?? 'error.genericMessage', apiError?.argsI18n ?? {});
    messageService.add({
      severity: 'error',
      summary: this.translateService.instant('error.genericTitle'),
      detail,
      life: DEFAULT_ERROR_LIFE,
    });
  }
}

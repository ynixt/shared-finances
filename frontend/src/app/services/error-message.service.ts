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
    if (error instanceof HttpErrorResponse) {
      const apiError = error.error as AppResponseErrorDto;

      messageService.add({
        severity: 'error',
        summary: this.translateService.instant('error.genericTitle'),
        detail: this.translateService.instant(apiError.messageI18n ?? 'error.genericMessage', apiError.argsI18n ?? {}),
        life: DEFAULT_ERROR_LIFE,
      });

      return;
    }

    messageService.add({
      severity: 'error',
      summary: this.translateService.instant('error.genericTitle'),
      detail: this.translateService.instant('error.genericMessage'),
      life: DEFAULT_ERROR_LIFE,
    });
  }
}

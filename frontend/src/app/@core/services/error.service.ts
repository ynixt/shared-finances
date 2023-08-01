import { Injectable } from '@angular/core';
import { TranslocoService } from '@ngneat/transloco';

@Injectable({
  providedIn: 'root',
})
export class ErrorService {
  constructor(private translocoService: TranslocoService) {}

  getInstantErrorMessage(error: any, message: string, messageWithDescription: string, args?: any, errorKey = 'error'): string {
    args ??= {};

    if (typeof error === 'string') {
      return this.translocoService.translate(messageWithDescription, { ...args, [errorKey]: error });
    }

    return this.translocoService.translate(message, args);
  }
}

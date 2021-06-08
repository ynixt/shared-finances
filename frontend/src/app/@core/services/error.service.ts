import { Injectable } from '@angular/core';
import { ApolloError } from '@apollo/client/errors';
import { TranslocoService } from '@ngneat/transloco';

@Injectable({
  providedIn: 'root',
})
export class ErrorService {
  constructor(private translocoService: TranslocoService) {}

  getInstantErrorMessage(error: any, message: string, messageWithDescription: string, args: any, errorKey = 'error'): string {
    args ??= {};

    if (typeof error === 'string') {
      return this.translocoService.translate(messageWithDescription, { ...args, [errorKey]: error });
    }

    if (error instanceof ApolloError) {
      return this.translocoService.translate(messageWithDescription, { ...args, [errorKey]: error.message });
    }

    return this.translocoService.translate(message, args);
  }
}

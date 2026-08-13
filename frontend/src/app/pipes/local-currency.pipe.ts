import { Injectable, Pipe, PipeTransform } from '@angular/core';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';

import { LocaleService } from '../services/locale.service';

@Pipe({ name: 'localCurrency', pure: false, standalone: true })
@UntilDestroy()
export class LocalCurrencyPipe implements PipeTransform {
  private locale = 'en-US';

  constructor(
    private localeService: LocaleService,
    private localCurrencyPipeService: LocalCurrencyPipeService,
  ) {
    this.localeService.locale$.pipe(untilDestroyed(this)).subscribe(l => (this.locale = l));
  }

  transform(value: number | null | undefined | string, currencyCode?: string, minimumFractionDigits = 2, maximumFractionDigits?: number) {
    return this.localCurrencyPipeService.transform(value, currencyCode, minimumFractionDigits, this.locale, maximumFractionDigits);
  }
}

@Injectable({ providedIn: 'root' })
export class LocalCurrencyPipeService {
  constructor(private localeService: LocaleService) {}

  transform(
    valueStr: number | null | undefined | string,
    currencyCode?: string,
    minimumFractionDigits = 2,
    locale?: string,
    maximumFractionDigits?: number,
  ): string {
    if (valueStr == null) return '';

    const value = typeof valueStr === 'string' ? parseFloat(valueStr) : valueStr;

    // Intl rejects a maximum below the minimum and caps fraction digits at 20.
    const fractionDigits =
      maximumFractionDigits == null
        ? { minimumFractionDigits }
        : { minimumFractionDigits, maximumFractionDigits: Math.min(Math.max(maximumFractionDigits, minimumFractionDigits), 20) };

    const code = currencyCode || 'USD';
    try {
      return new Intl.NumberFormat(locale ?? this.localeService.locale, {
        style: 'currency',
        currency: code,
        ...fractionDigits,
      })
        .format(value)
        .replace('-', '- ');
    } catch (error) {
      return (
        new Intl.NumberFormat(locale ?? this.localeService.locale, {
          style: 'currency',
          currency: 'brl',
          ...fractionDigits,
        })
          .format(value)
          .replace('-', '- ')
          // pt-BR renders BRL as "R$ 1,00" and en-US as "R$1.00"; absorbing the separator keeps
          // exactly one space after the substituted code in both.
          .replace(/R\$\s?/, `${currencyCode} `)
      );
    }
  }
}

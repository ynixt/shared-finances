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

  transform(value: number | null | undefined, currencyCode?: string, minimumFractionDigits = 2) {
    return this.localCurrencyPipeService.transform(value, currencyCode, minimumFractionDigits, this.locale);
  }
}

@Injectable({ providedIn: 'root' })
export class LocalCurrencyPipeService {
  constructor(private localeService: LocaleService) {}

  transform(value: number | null | undefined, currencyCode?: string, minimumFractionDigits = 2, locale?: string): string {
    if (value == null) return '';
    const code = currencyCode || 'USD';
    return new Intl.NumberFormat(locale ?? this.localeService.locale, {
      style: 'currency',
      currency: code,
      minimumFractionDigits,
    }).format(value);
  }
}

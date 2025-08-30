import { Pipe, PipeTransform } from '@angular/core';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';

import { LocaleService } from '../services/locale.service';

@Pipe({ name: 'localCurrency', pure: false, standalone: true })
@UntilDestroy()
export class LocalCurrencyPipe implements PipeTransform {
  private locale = 'en-US';

  constructor(private localeService: LocaleService) {
    this.localeService.locale$.pipe(untilDestroyed(this)).subscribe(l => (this.locale = l));
  }

  transform(value: number | null | undefined, currencyCode?: string, minimumFractionDigits = 2) {
    if (value == null) return '';
    const code = currencyCode || 'USD';
    return new Intl.NumberFormat(this.locale, {
      style: 'currency',
      currency: code,
      minimumFractionDigits,
    }).format(value);
  }
}

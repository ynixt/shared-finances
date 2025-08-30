import { Pipe, PipeTransform } from '@angular/core';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';

import { LocaleService } from '../services/locale.service';

@Pipe({ name: 'localNumber', pure: false, standalone: true })
@UntilDestroy()
export class LocalNumberPipe implements PipeTransform {
  private locale = 'en-US';

  constructor(private localeService: LocaleService) {
    this.localeService.locale$.pipe(untilDestroyed(this)).subscribe(l => (this.locale = l));
  }

  transform(value: number | null | undefined, options?: Intl.NumberFormatOptions) {
    if (value == null) return '';
    return new Intl.NumberFormat(this.locale, options).format(value);
  }
}

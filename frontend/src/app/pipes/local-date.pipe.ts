import { formatDate } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';

import { LocaleService } from '../services/locale.service';

@Pipe({ name: 'localDate', pure: false, standalone: true })
@UntilDestroy()
export class LocalDatePipe implements PipeTransform {
  private locale = 'en-US';

  constructor(private localeService: LocaleService) {
    this.localeService.locale$.pipe(untilDestroyed(this)).subscribe(l => (this.locale = l));
  }

  transform(value: string | number | null | undefined, format: string = 'short') {
    if (value == null) return '';
    return formatDate(value, format, this.locale);
  }
}

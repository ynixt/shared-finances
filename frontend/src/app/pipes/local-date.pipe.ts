import { formatDate } from '@angular/common';
import { Injectable, Pipe, PipeTransform } from '@angular/core';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';

import dayjs from 'dayjs';

import { LocaleService } from '../services/locale.service';

@Pipe({ name: 'localDate', pure: false, standalone: true })
@UntilDestroy()
export class LocalDatePipe implements PipeTransform {
  private locale = 'en-US';

  constructor(
    private localeService: LocaleService,
    private localDatePipeService: LocalDatePipeService,
  ) {
    this.localeService.locale$.pipe(untilDestroyed(this)).subscribe(l => (this.locale = l));
  }

  transform(value: string | number | Date | dayjs.Dayjs | null | undefined, format: string = 'short') {
    return this.localDatePipeService.transform(value, format, this.locale);
  }
}

@Injectable({ providedIn: 'root' })
export class LocalDatePipeService {
  constructor(private localeService: LocaleService) {}

  transform(value: string | number | Date | dayjs.Dayjs | null | undefined, format: string = 'short', locale?: string): string {
    if (value == null) return '';
    if (dayjs.isDayjs(value)) return formatDate(value.toDate(), format, locale ?? this.localeService.locale);
    return formatDate(value, format, locale ?? this.localeService.locale);
  }
}

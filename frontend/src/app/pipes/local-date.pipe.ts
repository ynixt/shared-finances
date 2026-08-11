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

  transform(value: string | number | Date | dayjs.Dayjs | null | undefined, format: string = 'short', timezone?: string) {
    return this.localDatePipeService.transform(value, format, this.locale, timezone);
  }
}

@Injectable({ providedIn: 'root' })
export class LocalDatePipeService {
  constructor(private localeService: LocaleService) {}

  transform(
    value: string | number | Date | dayjs.Dayjs | null | undefined,
    format: string = 'short',
    locale?: string,
    timezone?: string,
  ): string {
    if (value == null) return '';
    const date = dayjs.isDayjs(value) ? value.toDate() : new Date(value);
    return formatDate(date, format, locale ?? this.localeService.locale, this.resolveTimezoneOffset(date, timezone));
  }

  private resolveTimezoneOffset(date: Date, timezone?: string): string | undefined {
    if (timezone == null || !timezone.includes('/')) return timezone;

    const offset = new Intl.DateTimeFormat('en-US', {
      timeZone: timezone,
      timeZoneName: 'longOffset',
    })
      .formatToParts(date)
      .find(part => part.type === 'timeZoneName')
      ?.value.replace('GMT', '');

    return offset === '' ? '+0000' : offset?.replace(':', '');
  }
}

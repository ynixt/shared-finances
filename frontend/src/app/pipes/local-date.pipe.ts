import { formatDate } from '@angular/common';
import { Injectable, Pipe, PipeTransform } from '@angular/core';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';

import dayjs from 'dayjs';
import timezone from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';

import { LocaleService } from '../services/locale.service';

dayjs.extend(utc);
dayjs.extend(timezone);

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
    let dateInDayJs = dayjs.isDayjs(value) ? value : dayjs(value);

    if (timezone != null) {
      dateInDayJs = dateInDayJs.tz(timezone);
    }

    return formatDate(dateInDayJs.toDate(), format, locale ?? this.localeService.locale);
  }
}

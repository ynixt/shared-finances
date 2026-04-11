import { Injectable, Pipe, PipeTransform } from '@angular/core';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';

import { LocaleService } from '../services/locale.service';

@Pipe({ name: 'localNumber', pure: false, standalone: true })
@UntilDestroy()
export class LocalNumberPipe implements PipeTransform {
  private locale = 'en-US';

  constructor(
    private localeService: LocaleService,
    private localNumberPipeService: LocalNumberPipeService,
  ) {
    this.localeService.locale$.pipe(untilDestroyed(this)).subscribe(l => (this.locale = l));
  }

  transform(value: number | null | undefined, options?: Intl.NumberFormatOptions) {
    return this.localNumberPipeService.transform(value, options, this.locale);
  }
}

@Injectable({ providedIn: 'root' })
export class LocalNumberPipeService {
  constructor(private localeService: LocaleService) {}

  transform(value: number | null | undefined, options?: Intl.NumberFormatOptions, locale?: string): string {
    if (value == null) return '';
    return new Intl.NumberFormat(locale ?? this.localeService.locale, options).format(value);
  }
}

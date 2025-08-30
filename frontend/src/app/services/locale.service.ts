import { registerLocaleData } from '@angular/common';
import { Injectable } from '@angular/core';

import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class LocaleService {
  private _locale$ = new BehaviorSubject<string>('en-US');
  locale$ = this._locale$.asObservable();

  get locale() {
    return this._locale$.value;
  }

  async setLocale(localeId: string): Promise<void> {
    if (localeId === this.locale) return;

    switch (localeId) {
      case 'pt-BR': {
        const m = await import('@angular/common/locales/pt');
        registerLocaleData(m.default || m, 'pt-BR');
        break;
      }
      case 'en-US': {
        const m = await import('@angular/common/locales/en');
        registerLocaleData(m.default || m, 'en-US');
        break;
      }
      default:
        console.warn('Locale not supported:', localeId);
        return this.setLocale('en-US');
    }

    this._locale$.next(localeId);
  }
}

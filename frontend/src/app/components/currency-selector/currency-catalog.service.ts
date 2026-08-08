import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { Observable, catchError, defer, map, of, shareReplay } from 'rxjs';

export interface CurrencyItem {
  code: string;
  name?: string;
  symbol: string;
  all: string;
}

@Injectable({ providedIn: 'root' })
export class CurrencyCatalogService {
  private readonly cache = new Map<string, Observable<CurrencyItem[]>>();

  constructor(private readonly http: HttpClient) {}

  getCurrencies(assetsUrl: string): Observable<CurrencyItem[]> {
    const cached = this.cache.get(assetsUrl);

    if (cached) {
      return cached;
    }

    const request = defer(() => this.http.get<Record<string, string>>(assetsUrl)).pipe(
      map(data => this.toCurrencyItems(data)),
      catchError(() => {
        this.cache.delete(assetsUrl);
        return of([]);
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

    this.cache.set(assetsUrl, request);
    return request;
  }

  private toCurrencyItems(data: Record<string, string>): CurrencyItem[] {
    return Object.entries(data)
      .map(([rawCode, name]) => {
        const code = rawCode.toUpperCase();
        const symbol = this.getIntlSymbol(code);

        return {
          code,
          name: name || undefined,
          symbol,
          all: `${code} ${name} ${symbol}`,
        };
      })
      .sort((a, b) => a.code.localeCompare(b.code));
  }

  private getIntlSymbol(code: string, locale = 'en-US'): string {
    try {
      const parts = new Intl.NumberFormat(locale, { style: 'currency', currency: code }).formatToParts(0);
      return parts.find(part => part.type === 'currency')?.value ?? code;
    } catch {
      return code;
    }
  }
}

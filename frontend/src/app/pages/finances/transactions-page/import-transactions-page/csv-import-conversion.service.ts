import { Injectable, inject } from '@angular/core';

import { ExchangeRateResolution, ExchangeRateResolveRequest, ExchangeRateService } from '../../services/exchange-rate.service';
import { ImportPreviewRow } from './import-transactions.models';
import { convertImportValue } from './import-transactions.utils';

export interface CsvImportConversionContext {
  displayCurrency(row: ImportPreviewRow): string;
  text(key: string, params?: Record<string, unknown>): string;
}

@Injectable()
export class CsvImportConversionService {
  private readonly exchangeRateService = inject(ExchangeRateService);
  private requestId = 0;

  async refresh(rows: ImportPreviewRow[], context: CsvImportConversionContext): Promise<void> {
    const requestId = ++this.requestId;
    const pendingRows = new Map<string, ImportPreviewRow[]>();

    rows.forEach(row => {
      const targetCurrency = context.displayCurrency(row);
      if (row.currencySource === 'FALLBACK') row.currency = targetCurrency;
      if (row.conversionTargetCurrency !== targetCurrency) this.reset(row);
      row.conversionTargetCurrency = targetCurrency;

      if (row.value == null) return;
      if (row.currency == null || row.currency === '') {
        row.conversionRate = undefined;
        row.convertedValue = undefined;
        row.convertedValueOverridden = false;
        row.conversionLoading = false;
        row.conversionError = context.text('conversion.selectValidCurrency');
        return;
      }
      if (row.currency === targetCurrency) {
        row.conversionRate = 1;
        row.convertedValue = row.value;
        row.convertedValueOverridden = false;
        row.conversionLoading = false;
        row.conversionError = undefined;
        return;
      }
      if (row.conversionRate != null && row.conversionRate > 0) {
        if (!row.convertedValueOverridden) row.convertedValue = convertImportValue(row.value, row.conversionRate);
        row.conversionLoading = false;
        row.conversionError = undefined;
        return;
      }
      if (row.convertedValueOverridden && row.convertedValue != null) {
        row.conversionLoading = false;
        row.conversionError = undefined;
        return;
      }
      if (row.date == null) return;
      row.conversionLoading = true;
      row.conversionError = undefined;
      const key = this.key(row.currency, targetCurrency, row.date);
      pendingRows.set(key, [...(pendingRows.get(key) ?? []), row]);
    });

    if (pendingRows.size === 0) return;
    const requests = Array.from(pendingRows.keys()).map(key => {
      const [fromCurrency, toCurrency, referenceDate] = key.split('|');
      return { fromCurrency, toCurrency, referenceDate } satisfies ExchangeRateResolveRequest;
    });

    try {
      const resolutions: ExchangeRateResolution[] = [];
      for (let index = 0; index < requests.length; index += 500) {
        resolutions.push(...(await this.exchangeRateService.resolve(requests.slice(index, index + 500))));
      }
      if (requestId !== this.requestId) return;
      const byKey = new Map(
        resolutions.map(resolution => [this.key(resolution.fromCurrency, resolution.toCurrency, resolution.referenceDate), resolution]),
      );
      pendingRows.forEach((pending, key) => {
        const resolution = byKey.get(key);
        pending.forEach(row => {
          row.conversionLoading = false;
          if (resolution?.rate == null || resolution.rate <= 0) {
            row.conversionRate = undefined;
            row.convertedValue = undefined;
            row.conversionError = context.text('conversion.rateUnavailable', {
              from: row.currency,
              to: row.conversionTargetCurrency,
            });
            return;
          }
          row.conversionRate = resolution.rate;
          row.convertedValue = convertImportValue(row.value, resolution.rate);
          row.convertedValueOverridden = false;
          row.conversionError = undefined;
        });
      });
    } catch {
      if (requestId !== this.requestId) return;
      pendingRows.forEach(pending =>
        pending.forEach(row => {
          row.conversionLoading = false;
          row.convertedValue = undefined;
          row.conversionError = context.text('conversion.lookupFailed');
        }),
      );
    }
  }

  reset(row: ImportPreviewRow): void {
    row.conversionRate = undefined;
    row.convertedValue = undefined;
    row.convertedValueOverridden = false;
    row.conversionLoading = false;
    row.conversionError = undefined;
  }

  private key(fromCurrency: string, toCurrency: string, referenceDate: string): string {
    return `${fromCurrency}|${toCurrency}|${referenceDate}`;
  }
}

export { CsvImportConversionService as ImportConversionService };

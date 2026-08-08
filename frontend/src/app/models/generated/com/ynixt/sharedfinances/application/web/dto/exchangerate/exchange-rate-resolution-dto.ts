/* eslint-disable */
/* tslint-disable */

export interface ExchangeRateResolutionDto {
  fromCurrency: string;
  quoteDate?: string | null;
  rate?: number | null;
  referenceDate: string;
  toCurrency: string;
}

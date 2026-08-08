import { mapHeadersByIndex, matchesExactHeaders } from './csv-layout-provider';
import type { CsvLayoutProvider } from './csv-layout-provider';

const signature = ['date', 'title', 'amount'];

export const nubankCreditCardV1LayoutProvider: CsvLayoutProvider = {
  id: 'nubank-credit-card-v1',
  matches: headers => matchesExactHeaders(headers, signature),
  detect: headers =>
    mapHeadersByIndex(headers, {
      date: 0,
      description: 1,
      value: 2,
    }),
};

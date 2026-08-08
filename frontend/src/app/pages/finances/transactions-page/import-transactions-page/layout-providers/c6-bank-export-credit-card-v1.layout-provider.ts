import { mapHeadersByIndex, matchesExactHeaders } from './csv-layout-provider';
import type { CsvLayoutProvider } from './csv-layout-provider';

const signature = [
  'data de compra',
  'nome no cartao',
  'final do cartao',
  'categoria',
  'descricao',
  'parcela',
  'valor (em us$)',
  'cotacao (em r$)',
  'valor (em r$)',
];

export const c6BankExportCreditCardV1LayoutProvider: CsvLayoutProvider = {
  id: 'c6-bank-export-credit-card-v1',
  matches: headers => matchesExactHeaders(headers, signature),
  detect: headers =>
    mapHeadersByIndex(headers, {
      date: 0,
      category: 3,
      description: 4,
      installment: 5,
      value: 8,
    }),
};

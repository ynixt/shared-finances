import { mapHeadersByIndex, matchesExactHeaders } from './csv-layout-provider';
import type { CsvColumnField, CsvLayoutProvider } from './csv-layout-provider';

const fields: CsvColumnField[] = [
  'origin',
  'date',
  'description',
  'value',
  'currency',
  'category',
  'group',
  'installment',
  'beneficiaries',
  'bill',
  'tags',
  'observations',
  'confirmed',
];

const portugueseSignature = [
  'origem',
  'data',
  'descricao',
  'valor',
  'moeda',
  'categoria',
  'grupo',
  'parcela',
  'beneficiarios',
  'fatura',
  'tags',
  'observacoes',
  'confirmado',
];

const englishSignature = [
  'origin',
  'date',
  'description',
  'amount',
  'currency',
  'category',
  'group',
  'installment',
  'beneficiaries',
  'bill',
  'tags',
  'observations',
  'confirmed',
];

export const sharedFinancesCsvTemplateV1LayoutProvider: CsvLayoutProvider = {
  id: 'shared-finances-csv-template-v1',
  matches: headers => matchesExactHeaders(headers, portugueseSignature) || matchesExactHeaders(headers, englishSignature),
  detect: headers => {
    return mapHeadersByIndex(headers, Object.fromEntries(fields.map((field, index) => [field, index])));
  },
};

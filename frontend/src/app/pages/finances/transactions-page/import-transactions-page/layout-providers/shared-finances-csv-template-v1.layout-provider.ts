import { mapHeadersByIndex, matchesExactHeaders } from './csv-layout-provider';
import type { CsvColumnField, CsvLayoutProvider } from './csv-layout-provider';

const fields: CsvColumnField[] = [
  'origin',
  'originName',
  'date',
  'description',
  'value',
  'currency',
  'category',
  'categoryName',
  'categoryConceptId',
  'group',
  'groupName',
  'installment',
  'beneficiaries',
  'bill',
  'tags',
  'observations',
  'confirmed',
  'transactionId',
  'transferId',
  'seriesId',
];

const portugueseSignature = [
  'origem',
  'nome_origem',
  'data',
  'descricao',
  'valor',
  'moeda',
  'categoria',
  'nome_categoria',
  'id_conceito_categoria',
  'grupo',
  'nome_grupo',
  'parcela',
  'beneficiarios',
  'fatura',
  'tags',
  'observacoes',
  'confirmado',
  'id transacao',
  'id transferencia',
  'id serie',
];

const englishSignature = [
  'origin',
  'origin_name',
  'date',
  'description',
  'value',
  'currency',
  'category',
  'category_name',
  'category_concept_id',
  'group',
  'group_name',
  'installment',
  'beneficiaries',
  'bill',
  'tags',
  'observations',
  'confirmed',
  'transaction id',
  'transfer id',
  'series id',
];

export const sharedFinancesCsvTemplateV1LayoutProvider: CsvLayoutProvider = {
  id: 'shared-finances-csv-template-v1',
  matches: headers => matchesExactHeaders(headers, portugueseSignature) || matchesExactHeaders(headers, englishSignature),
  detect: headers => {
    return mapHeadersByIndex(headers, Object.fromEntries(fields.map((field, index) => [field, index])));
  },
};

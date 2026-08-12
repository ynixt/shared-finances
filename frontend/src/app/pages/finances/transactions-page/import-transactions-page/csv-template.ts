import { CsvColumnField } from './csv-statement-parser';

export interface LocalizedCsvTemplate {
  content: string;
  fileName: string;
}

const translationPrefix = 'financesPage.transactionsPage.importPage.csvTemplate';

const templateFields: CsvColumnField[] = [
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

export function buildLocalizedCsvTemplate(translate: (key: string) => string): LocalizedCsvTemplate {
  const headers = templateFields.map(field => translate(`${translationPrefix}.columns.${field}`));

  return {
    fileName: translate(`${translationPrefix}.fileName`),
    content: `\uFEFF${headers.map(escapeCsvCell).join(';')}\r\n`,
  };
}

function escapeCsvCell(value: string): string {
  const escaped = value.replace(/"/g, '""');
  return /[;"\r\n]/.test(value) ? `"${escaped}"` : escaped;
}

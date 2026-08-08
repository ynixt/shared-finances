export type CsvColumnField =
  | 'origin'
  | 'date'
  | 'description'
  | 'value'
  | 'credit'
  | 'debit'
  | 'type'
  | 'category'
  | 'currency'
  | 'beneficiaries'
  | 'transactionId'
  | 'installment'
  | 'group'
  | 'bill'
  | 'tags'
  | 'observations'
  | 'confirmed';

export type CsvColumnMapping = Partial<Record<CsvColumnField, string>>;

export interface CsvLayoutDetection {
  mapping: CsvColumnMapping;
  providerId?: string;
}

export interface CsvLayoutProvider {
  readonly id: string;
  detect(headers: string[]): CsvColumnMapping;
  matches(headers: string[]): boolean;
}

export function normalizeCsvHeader(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .toLowerCase();
}

export function matchesExactHeaders(headers: string[], expectedHeaders: string[]): boolean {
  return (
    headers.length === expectedHeaders.length && headers.every((header, index) => normalizeCsvHeader(header) === expectedHeaders[index])
  );
}

export function mapHeadersByIndex(headers: string[], indexes: Partial<Record<CsvColumnField, number>>): CsvColumnMapping {
  return Object.fromEntries(
    Object.entries(indexes)
      .filter((entry): entry is [string, number] => typeof entry[1] === 'number')
      .map(([field, index]) => [field, headers[index]] as const)
      .filter(([, header]) => header != null),
  ) as CsvColumnMapping;
}

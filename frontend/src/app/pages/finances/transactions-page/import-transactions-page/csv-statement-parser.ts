import { normalizeCsvHeader } from './layout-providers/csv-layout-provider';
import type { CsvColumnMapping } from './layout-providers/csv-layout-provider';
import { detectCsvLayout } from './layout-providers/csv-layout-providers';

export type { CsvColumnField, CsvColumnMapping } from './layout-providers/csv-layout-provider';

export type CsvDateFormat = 'AUTO' | 'DD/MM/YYYY' | 'MM/DD/YYYY' | 'YYYY-MM-DD';

export interface CsvParseOptions {
  dateFormat: CsvDateFormat;
  decimalSeparator: '.' | ',';
  delimiter: string;
}

export interface ParsedCsv {
  detectedDateFormat: Exclude<CsvDateFormat, 'AUTO'>;
  headers: string[];
  layoutProviderId?: string;
  mapping: CsvColumnMapping;
  rows: Record<string, string>[];
}

export function parseCsv(text: string, options: CsvParseOptions): ParsedCsv {
  if (options.delimiter.length !== 1) {
    throw new Error('O delimitador deve ter exatamente um caractere.');
  }

  const matrix = tokenizeCsv(text.replace(/^\uFEFF/, ''), options.delimiter).filter(row => row.some(cell => cell.trim().length > 0));
  if (matrix.length < 2) {
    throw new Error('O CSV precisa ter um cabeçalho e ao menos uma linha de dados.');
  }

  const headers = matrix[0].map((header, index) => header.trim() || `coluna_${index + 1}`);
  if (new Set(headers).size !== headers.length) {
    throw new Error('O CSV possui cabeçalhos de coluna duplicados.');
  }

  const rows = matrix.slice(1).map(values => Object.fromEntries(headers.map((header, index) => [header, (values[index] ?? '').trim()])));
  const layout = detectCsvLayout(headers);
  const mapping = layout.mapping;
  const dateSamples = mapping.date == null ? [] : rows.map(row => row[mapping.date!] ?? '').filter(Boolean);

  return {
    headers,
    layoutProviderId: layout.providerId,
    mapping,
    rows,
    detectedDateFormat: options.dateFormat === 'AUTO' ? detectDateFormat(dateSamples) : options.dateFormat,
  };
}

function tokenizeCsv(text: string, delimiter: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let value = '';
  let quoted = false;

  for (let index = 0; index < text.length; index++) {
    const character = text[index];
    if (quoted) {
      if (character === '"' && text[index + 1] === '"') {
        value += '"';
        index++;
      } else if (character === '"') {
        quoted = false;
      } else {
        value += character;
      }
      continue;
    }

    if (character === '"') {
      quoted = true;
    } else if (character === delimiter) {
      row.push(value);
      value = '';
    } else if (character === '\n') {
      row.push(value.replace(/\r$/, ''));
      rows.push(row);
      row = [];
      value = '';
    } else {
      value += character;
    }
  }

  if (quoted) {
    throw new Error('O CSV contém uma célula entre aspas que não foi fechada.');
  }
  if (value.length > 0 || row.length > 0) {
    row.push(value.replace(/\r$/, ''));
    rows.push(row);
  }
  return rows;
}

export function detectColumnMapping(headers: string[]): CsvColumnMapping {
  return detectCsvLayout(headers).mapping;
}

export function normalizeHeader(value: string): string {
  return normalizeCsvHeader(value);
}

export function detectDateFormat(samples: string[]): Exclude<CsvDateFormat, 'AUTO'> {
  if (samples.some(sample => /^\d{4}-\d{2}-\d{2}$/.test(sample.trim()))) {
    return 'YYYY-MM-DD';
  }

  let ddMmEvidence = 0;
  let mmDdEvidence = 0;
  samples.forEach(sample => {
    const match = sample.trim().match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
    if (match == null) return;
    const first = Number(match[1]);
    const second = Number(match[2]);
    if (first > 12 && second <= 12) ddMmEvidence++;
    if (second > 12 && first <= 12) mmDdEvidence++;
  });
  return mmDdEvidence > ddMmEvidence ? 'MM/DD/YYYY' : 'DD/MM/YYYY';
}

export function parseCsvDate(value: string, format: Exclude<CsvDateFormat, 'AUTO'>): string | null {
  const trimmed = value.trim();
  const match = format === 'YYYY-MM-DD' ? trimmed.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/) : trimmed.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (match == null) return null;

  const [year, month, day] =
    format === 'YYYY-MM-DD'
      ? [Number(match[1]), Number(match[2]), Number(match[3])]
      : format === 'DD/MM/YYYY'
        ? [Number(match[3]), Number(match[2]), Number(match[1])]
        : [Number(match[3]), Number(match[1]), Number(match[2])];
  const date = new Date(Date.UTC(year, month - 1, day));
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) {
    return null;
  }
  return `${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`;
}

export function parseCsvNumber(value: string, decimalSeparator: '.' | ','): number | null {
  const normalized =
    decimalSeparator === ',' ? value.replace(/\s/g, '').replace(/\./g, '').replace(',', '.') : value.replace(/\s/g, '').replace(/,/g, '');
  if (normalized.trim() === '') return null;
  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : null;
}

export function parseInstallment(value: string | undefined): { current: number; total: number } | null {
  const match = value?.trim().match(/^(\d+)\s*\/\s*(\d+)$/);
  if (match == null) return null;
  const current = Number(match[1]);
  const total = Number(match[2]);
  return current >= 1 && current <= total ? { current, total } : null;
}

export async function sha256File(file: File): Promise<string> {
  const bytes = await file.arrayBuffer();
  const digest = await crypto.subtle.digest('SHA-256', bytes);
  return Array.from(new Uint8Array(digest))
    .map(value => value.toString(16).padStart(2, '0'))
    .join('');
}

export function parseBeneficiaries(value: string | undefined): Array<{ email: string; benefitPercent: number }> {
  if (value == null || value.trim() === '') return [];
  return value
    .split('|')
    .map(item => item.trim())
    .map(item => {
      const separator = item.lastIndexOf(':');
      return {
        email: separator < 0 ? '' : item.slice(0, separator).trim().toLowerCase(),
        benefitPercent: separator < 0 ? Number.NaN : Number(item.slice(separator + 1)),
      };
    })
    .filter(item => item.email.length > 0 && Number.isFinite(item.benefitPercent));
}

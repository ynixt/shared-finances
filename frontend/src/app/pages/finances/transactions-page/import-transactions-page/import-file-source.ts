export type ImportFileFormat = 'CSV' | 'OFX';

export interface ParsedImportSourceRow {
  currency?: string;
  date?: string;
  externalTransactionId?: string;
  name?: string;
  observations?: string;
  raw: Record<string, string>;
  sourceStatementKey?: string;
  value?: number;
}

export interface ParsedImportSourceStatement {
  accountId: string;
  currency?: string;
  institutionId?: string;
  key: string;
  kind: 'BANK' | 'CREDIT_CARD';
  maskedAccountId: string;
  pendingCount: number;
  rows: ParsedImportSourceRow[];
}

export interface ImportSourceAdapter<TParsed> {
  readonly format: ImportFileFormat;
  parse(bytes: ArrayBuffer, maxLines: number): TParsed;
}

export function detectImportFileFormat(bytes: ArrayBuffer, fileName = ''): ImportFileFormat {
  const prefix = new TextDecoder('windows-1252')
    .decode(bytes.slice(0, 4096))
    .replace(/^\uFEFF/, '')
    .trimStart();
  if (/^(?:OFXHEADER\s*:|<\?xml\b[^>]*>\s*<OFX\b|<OFX\b)/i.test(prefix)) return 'OFX';
  if (/\.ofx$/i.test(fileName)) return 'OFX';
  return 'CSV';
}

export function decodeCsvBytes(bytes: ArrayBuffer): string {
  return new TextDecoder('utf-8').decode(bytes);
}

export async function sha256Bytes(bytes: ArrayBuffer): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', bytes);
  return Array.from(new Uint8Array(digest))
    .map(value => value.toString(16).padStart(2, '0'))
    .join('');
}

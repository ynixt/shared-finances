import { ParsedImportSourceRow, ParsedImportSourceStatement } from './import-file-source';

export type OfxParseErrorCode = 'lineLimitExceeded' | 'malformedOfx' | 'noPostedTransactions' | 'unsupportedEncoding';

export class OfxParseError extends Error {
  constructor(
    readonly code: OfxParseErrorCode,
    readonly params?: Record<string, unknown>,
  ) {
    super(code);
  }
}

export interface ParsedOfx {
  pendingCount: number;
  statements: ParsedImportSourceStatement[];
}

interface OfxNode {
  children: OfxNode[];
  name: string;
  text?: string;
}

const OFX_AGGREGATES = new Set([
  'OFX',
  'SIGNONMSGSRSV1',
  'SONRS',
  'STATUS',
  'FI',
  'BANKMSGSRSV1',
  'STMTTRNRS',
  'STMTRS',
  'BANKACCTFROM',
  'BANKTRANLIST',
  'BANKTRANLISTP',
  'STMTTRN',
  'STMTTRNP',
  'CCSTMTMSGSRSV1',
  'CCSTMTTRNRS',
  'CCSTMTRS',
  'CCACCTFROM',
  'CURRENCY',
  'ORIGCURRENCY',
  'INVBANKTRAN',
]);

const MAX_DEPTH = 64;
const MIN_TOKEN_LIMIT = 10_000;

export function parseOfx(bytes: ArrayBuffer, maxLines: number): ParsedOfx {
  const decoded = decodeOfx(bytes);
  const root = decoded.xml ? parseXml(decoded.text) : parseSgml(decoded.text, maxLines);
  const statements = [...extractStatements(root, 'STMTRS', 'BANK'), ...extractStatements(root, 'CCSTMTRS', 'CREDIT_CARD')];
  const lineCount = statements.reduce((total, statement) => total + statement.rows.length, 0);
  const pendingCount = statements.reduce((total, statement) => total + statement.pendingCount, 0);
  if (lineCount > maxLines) throw new OfxParseError('lineLimitExceeded', { maxLines });
  if (lineCount === 0) throw new OfxParseError('noPostedTransactions', { pendingCount });
  return { statements, pendingCount };
}

function decodeOfx(bytes: ArrayBuffer): { text: string; xml: boolean } {
  const prefix = new TextDecoder('windows-1252').decode(bytes.slice(0, 4096)).replace(/^\uFEFF/, '');
  const xml = /^\s*<\?xml\b/i.test(prefix) || /^\s*<OFX\b/i.test(prefix);
  const xmlEncoding = prefix.match(/<\?xml\b[^>]*\bencoding\s*=\s*["']([^"']+)["']/i)?.[1];
  const headerEncoding = prefix.match(/(?:^|\r?\n)ENCODING\s*:\s*([^\r\n]+)/i)?.[1]?.trim();
  const headerCharset = prefix.match(/(?:^|\r?\n)CHARSET\s*:\s*([^\r\n]+)/i)?.[1]?.trim();
  const label = resolveEncoding(xmlEncoding, headerEncoding, headerCharset, xml);
  try {
    return { text: new TextDecoder(label, { fatal: true }).decode(bytes).replace(/^\uFEFF/, ''), xml };
  } catch (error) {
    if (error instanceof RangeError) throw new OfxParseError('unsupportedEncoding');
    throw new OfxParseError('malformedOfx');
  }
}

function resolveEncoding(xmlEncoding?: string, headerEncoding?: string, headerCharset?: string, xml = false): string {
  const declared = [xmlEncoding, headerCharset, headerEncoding]
    .map(value => value?.trim().toUpperCase())
    .filter((value): value is string => value != null && value !== '');
  if (xmlEncoding != null && headerEncoding != null && normalizeEncoding(xmlEncoding) !== normalizeEncoding(headerEncoding)) {
    throw new OfxParseError('unsupportedEncoding');
  }
  if (!xml && declared.length === 0) return 'windows-1252';
  return normalizeEncoding(declared[0] ?? 'UTF-8');
}

function normalizeEncoding(value: string): string {
  const normalized = value.trim().toUpperCase().replace(/_/g, '-');
  if (['UTF-8', 'UTF8', 'UNICODE'].includes(normalized)) return 'utf-8';
  if (['USASCII', 'US-ASCII', 'ASCII', '1252', 'CP1252', 'WINDOWS-1252'].includes(normalized)) return 'windows-1252';
  if (['ISO-8859-1', 'ISO8859-1', 'LATIN1'].includes(normalized)) return 'iso-8859-1';
  throw new OfxParseError('unsupportedEncoding', { encoding: value });
}

function parseXml(text: string): OfxNode {
  if (/<!DOCTYPE/i.test(text)) throw new OfxParseError('malformedOfx');
  const document = new DOMParser().parseFromString(text, 'application/xml');
  if (document.querySelector('parsererror') != null || document.documentElement.tagName.toUpperCase() !== 'OFX') {
    throw new OfxParseError('malformedOfx');
  }
  return elementToNode(document.documentElement, 0);
}

function elementToNode(element: Element, depth: number): OfxNode {
  if (depth > MAX_DEPTH) throw new OfxParseError('malformedOfx');
  const children = Array.from(element.children).map(child => elementToNode(child, depth + 1));
  return {
    name: element.tagName.toUpperCase(),
    children,
    text: children.length === 0 ? element.textContent?.trim() : undefined,
  };
}

function parseSgml(text: string, maxLines: number): OfxNode {
  if (/<!DOCTYPE/i.test(text)) throw new OfxParseError('malformedOfx');
  const bodyIndex = text.search(/<OFX\b/i);
  if (bodyIndex < 0) throw new OfxParseError('malformedOfx');
  const body = text.slice(bodyIndex);
  const root: OfxNode = { name: '__ROOT__', children: [] };
  const stack: OfxNode[] = [root];
  const tagPattern = /<\s*(\/?)\s*([A-Za-z0-9_.:-]+)(?:\s+[^>]*)?>/g;
  const tokenLimit = Math.max(MIN_TOKEN_LIMIT, maxLines * 64);
  let openScalar: { node: OfxNode; valueStart: number } | undefined;
  let tokens = 0;
  let match: RegExpExecArray | null;

  while ((match = tagPattern.exec(body)) != null) {
    if (++tokens > tokenLimit) throw new OfxParseError('lineLimitExceeded', { maxLines });
    const closing = match[1] === '/';
    const name = match[2].toUpperCase();
    if (openScalar != null) {
      const scalar = openScalar;
      scalar.node.text = decodeEntities(body.slice(scalar.valueStart, match.index).trim());
      openScalar = undefined;
      if (closing && scalar.node.name === name) continue;
    }
    if (closing) {
      if (stack.length === 1 || stack.at(-1)?.name !== name) throw new OfxParseError('malformedOfx');
      stack.pop();
      continue;
    }
    const node: OfxNode = { name, children: [] };
    stack.at(-1)!.children.push(node);
    const followedByTag = /^\s*</.test(body.slice(tagPattern.lastIndex));
    if (OFX_AGGREGATES.has(name) || followedByTag) {
      stack.push(node);
      if (stack.length > MAX_DEPTH) throw new OfxParseError('malformedOfx');
    } else {
      openScalar = { node, valueStart: tagPattern.lastIndex };
    }
  }
  if (openScalar != null) openScalar.node.text = decodeEntities(body.slice(openScalar.valueStart).trim());
  if (stack.length !== 1 || root.children.length !== 1 || root.children[0].name !== 'OFX') {
    throw new OfxParseError('malformedOfx');
  }
  return root.children[0];
}

function decodeEntities(value: string): string {
  return value.replace(/&(#x[0-9a-f]+|#\d+|amp|lt|gt|quot|apos);/gi, (_, entity: string) => {
    const normalized = entity.toLowerCase();
    if (normalized === 'amp') return '&';
    if (normalized === 'lt') return '<';
    if (normalized === 'gt') return '>';
    if (normalized === 'quot') return '"';
    if (normalized === 'apos') return "'";
    const radix = normalized.startsWith('#x') ? 16 : 10;
    const number = Number.parseInt(normalized.replace(/^#x?/, ''), radix);
    return Number.isFinite(number) ? String.fromCodePoint(number) : '';
  });
}

function extractStatements(
  root: OfxNode,
  statementTag: 'STMTRS' | 'CCSTMTRS',
  kind: 'BANK' | 'CREDIT_CARD',
): ParsedImportSourceStatement[] {
  return findAll(root, statementTag).map((statement, index) => {
    const accountAggregate = child(statement, kind === 'BANK' ? 'BANKACCTFROM' : 'CCACCTFROM');
    const accountId = textChild(accountAggregate, 'ACCTID') ?? '';
    const institutionId = textChild(accountAggregate, 'BANKID');
    const currency = textChild(statement, 'CURDEF')?.toUpperCase();
    const key = `${kind}:${institutionId ?? ''}:${accountId}:${index}`;
    const posted = child(statement, 'BANKTRANLIST');
    const rows = (posted == null ? [] : posted.children.filter(node => node.name === 'STMTTRN')).map((transaction, rowIndex) =>
      normalizeTransaction(transaction, key, currency, rowIndex),
    );
    const pendingCount = findAll(statement, 'STMTTRNP').length;
    return {
      accountId,
      currency,
      institutionId,
      key,
      kind,
      maskedAccountId: maskAccountId(accountId),
      pendingCount,
      rows,
    };
  });
}

function normalizeTransaction(
  transaction: OfxNode,
  statementKey: string,
  statementCurrency: string | undefined,
  rowIndex: number,
): ParsedImportSourceRow {
  const postedAt = textChild(transaction, 'DTPOSTED');
  const amountText = textChild(transaction, 'TRNAMT');
  const name = textChild(transaction, 'NAME')?.trim();
  const memo = textChild(transaction, 'MEMO')?.trim();
  const transactionType = textChild(transaction, 'TRNTYPE')?.trim();
  const externalTransactionId = textChild(transaction, 'FITID')?.trim() || undefined;
  const date = parseOfxDate(postedAt);
  const parsedAmount = amountText == null || amountText.trim() === '' ? Number.NaN : Number(amountText.trim().replace(',', '.'));
  const value = Number.isFinite(parsedAmount) ? parsedAmount : undefined;
  const transactionCurrency =
    textChild(child(transaction, 'CURRENCY'), 'CURSYM') ?? textChild(child(transaction, 'ORIGCURRENCY'), 'CURSYM');
  const description = name || memo || transactionType || undefined;
  const observations = memo != null && memo !== '' && memo !== description ? memo : undefined;
  return {
    currency: transactionCurrency?.toUpperCase() ?? statementCurrency,
    date,
    externalTransactionId,
    name: description,
    observations,
    sourceStatementKey: statementKey,
    value,
    raw: {
      date: postedAt ?? '',
      description: description ?? '',
      value: amountText ?? '',
      currency: transactionCurrency ?? statementCurrency ?? '',
      observations: observations ?? '',
      transactionId: externalTransactionId ?? '',
      transactionType: transactionType ?? '',
      sourceRow: String(rowIndex + 1),
    },
  };
}

function parseOfxDate(value?: string): string | undefined {
  const match = value?.trim().match(/^(\d{4})(\d{2})(\d{2})/);
  if (match == null) return undefined;
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const date = new Date(Date.UTC(year, month - 1, day));
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return undefined;
  return `${match[1]}-${match[2]}-${match[3]}`;
}

function maskAccountId(accountId: string): string {
  const suffix = accountId.trim().slice(-4);
  return suffix === '' ? '••••' : `•••• ${suffix}`;
}

function child(node: OfxNode | undefined, name: string): OfxNode | undefined {
  return node?.children.find(candidate => candidate.name === name);
}

function textChild(node: OfxNode | undefined, name: string): string | undefined {
  return child(node, name)?.text?.trim() || undefined;
}

function findAll(node: OfxNode, name: string): OfxNode[] {
  const matches = node.name === name ? [node] : [];
  return node.children.reduce<OfxNode[]>((all, current) => [...all, ...findAll(current, name)], matches);
}

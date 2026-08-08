import { describe, expect, it } from 'vitest';

import { detectImportFileFormat } from './import-file-source';
import { OfxParseError, parseOfx } from './ofx-statement-parser';

const encoder = new TextEncoder();

describe('OFX statement parser', () => {
  it('parses OFX 1.x bank transactions with omitted scalar closing tags', () => {
    const parsed = parseOfx(
      encoder.encode(
        `OFXHEADER:100\nDATA:OFXSGML\nVERSION:102\nENCODING:USASCII\nCHARSET:1252\n\n<OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><CURDEF>BRL<BANKACCTFROM><BANKID>001<ACCTID>123456789</BANKACCTFROM><BANKTRANLIST><STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260807120000[-3:BRT]<TRNAMT>-42.50<FITID>bank-001<NAME>Mercado<MEMO>Compra semanal</STMTTRN></BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>`,
      ).buffer,
      100,
    );

    expect(parsed.pendingCount).toBe(0);
    expect(parsed.statements).toHaveLength(1);
    expect(parsed.statements[0]).toMatchObject({ kind: 'BANK', currency: 'BRL', maskedAccountId: '•••• 6789' });
    expect(parsed.statements[0].rows[0]).toMatchObject({
      date: '2026-08-07',
      value: -42.5,
      name: 'Mercado',
      observations: 'Compra semanal',
      externalTransactionId: 'bank-001',
    });
  });

  it('parses OFX 2.x bank and card statements and omits pending rows', () => {
    const parsed = parseOfx(
      encoder.encode(
        `<?xml version="1.0" encoding="UTF-8"?><OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><CURDEF>BRL</CURDEF><BANKACCTFROM><ACCTID>11112222</ACCTID></BANKACCTFROM><BANKTRANLIST><STMTTRN><DTPOSTED>20260801</DTPOSTED><TRNAMT>100</TRNAMT><FITID>bank</FITID><MEMO>Recebido</MEMO></STMTTRN></BANKTRANLIST><BANKTRANLISTP><STMTTRNP><DTTRAN>20260808</DTTRAN><TRNAMT>-10</TRNAMT></STMTTRNP></BANKTRANLISTP></STMTRS></STMTTRNRS></BANKMSGSRSV1><CREDITCARDMSGSRSV1><CCSTMTTRNRS><CCSTMTRS><CURDEF>USD</CURDEF><CCACCTFROM><ACCTID>99998888</ACCTID></CCACCTFROM><BANKTRANLIST><STMTTRN><DTPOSTED>20260731</DTPOSTED><TRNAMT>-12.34</TRNAMT><FITID>card</FITID><NAME>Coffee</NAME><MEMO>Downtown</MEMO></STMTTRN></BANKTRANLIST></CCSTMTRS></CCSTMTTRNRS></CREDITCARDMSGSRSV1></OFX>`,
      ).buffer,
      100,
    );

    expect(parsed.pendingCount).toBe(1);
    expect(parsed.statements.map(statement => statement.kind)).toEqual(['BANK', 'CREDIT_CARD']);
    expect(parsed.statements.flatMap(statement => statement.rows)).toHaveLength(2);
    expect(parsed.statements[0].rows[0]).toMatchObject({ name: 'Recebido', observations: undefined });
  });

  it('honors Windows-1252 declarations', () => {
    const prefix =
      'OFXHEADER:100\nDATA:OFXSGML\nVERSION:102\nENCODING:USASCII\nCHARSET:1252\n\n<OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><CURDEF>BRL<BANKACCTFROM><ACCTID>1234</BANKACCTFROM><BANKTRANLIST><STMTTRN><DTPOSTED>20260801<TRNAMT>-1<FITID>1<NAME>A';
    const suffix = 'aí</STMTTRN></BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>';
    const bytes = Uint8Array.from(
      [...prefix]
        .map(char => char.charCodeAt(0))
        .concat(
          [0xe7],
          [...suffix].map(char => char.charCodeAt(0)),
        ),
    );

    expect(parseOfx(bytes.buffer, 10).statements[0].rows[0].name).toBe('Açaí');
  });

  it('rejects malformed, unsupported-only, and over-limit documents', () => {
    expect(() => parseOfx(encoder.encode('<OFX><BANKMSGSRSV1></OFX>').buffer, 10)).toThrow(OfxParseError);
    expect(() => parseOfx(encoder.encode('<?xml version="1.0"?><OFX><INVSTMTMSGSRSV1 /></OFX>').buffer, 10)).toThrowError(
      expect.objectContaining({ code: 'noPostedTransactions' }),
    );
    const twoRows =
      '<?xml version="1.0"?><OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><BANKACCTFROM><ACCTID>1</ACCTID></BANKACCTFROM><BANKTRANLIST><STMTTRN><DTPOSTED>20260801</DTPOSTED><TRNAMT>1</TRNAMT></STMTTRN><STMTTRN><DTPOSTED>20260802</DTPOSTED><TRNAMT>2</TRNAMT></STMTTRN></BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>';
    expect(() => parseOfx(encoder.encode(twoRows).buffer, 1)).toThrowError(expect.objectContaining({ code: 'lineLimitExceeded' }));
  });

  it('handles tag-case and multiline values while preserving invalid rows for preview correction', () => {
    const parsed = parseOfx(
      encoder.encode(
        `OFXHEADER:100\nDATA:OFXSGML\nVERSION:102\nENCODING:UTF-8\n\n<ofx><bankmsgsrsv1><stmttrnrs><stmtrs><curdef>BRL<bankacctfrom><acctid>1234</bankacctfrom><banktranlist><stmttrn><dtposted>invalid<trnamt>invalid<name>Loja\nCentro</stmttrn></banktranlist></stmtrs></stmttrnrs></bankmsgsrsv1></ofx>`,
      ).buffer,
      10,
    );

    expect(parsed.statements[0].rows[0]).toMatchObject({ date: undefined, value: undefined, name: 'Loja\nCentro' });
    expect(() => parseOfx(encoder.encode('<?xml version="1.0"?><!DOCTYPE OFX><OFX />').buffer, 10)).toThrowError(
      expect.objectContaining({ code: 'malformedOfx' }),
    );
  });

  it('detects content before extension and treats an invalid .ofx as OFX for a specific error', () => {
    expect(detectImportFileFormat(encoder.encode('<OFX></OFX>').buffer, 'statement.csv')).toBe('OFX');
    expect(detectImportFileFormat(encoder.encode('not ofx').buffer, 'statement.ofx')).toBe('OFX');
    expect(detectImportFileFormat(encoder.encode('date;amount\n2026-01-01;1').buffer, 'statement.csv')).toBe('CSV');
  });
});

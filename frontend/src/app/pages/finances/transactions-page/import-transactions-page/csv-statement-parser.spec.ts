import { describe, expect, it } from 'vitest';

import { parseBeneficiaries, parseCsv, parseCsvDate, parseCsvNumber, parseInstallment } from './csv-statement-parser';
import { detectCsvLayout } from './layout-providers/csv-layout-providers';

describe('CSV statement parser', () => {
  it('does not infer mappings from a partial unknown layout', () => {
    const csv =
      '\uFEFFData de Compra;Nome no Cartão;Valor (em R$);Parcela\n' +
      '22/04/2026;"MERCADO; BAIRRO";123.45;4/6\n' +
      '23/04/2026;PAGAMENTO;-50.00;Única\n';
    const parsed = parseCsv(csv, { delimiter: ';', decimalSeparator: '.', dateFormat: 'AUTO' });
    const mapping = parsed.mapping;

    expect(parsed.detectedDateFormat).toBe('DD/MM/YYYY');
    expect(parsed.layoutProviderId).toBeUndefined();
    expect(mapping).toEqual({});
    expect(parsed.rows[0]['Nome no Cartão']).toBe('MERCADO; BAIRRO');
    expect(parseInstallment(parsed.rows[0]['Parcela'])).toEqual({ current: 4, total: 6 });
    expect(parseCsvNumber(parsed.rows[1]['Valor (em R$)'], '.')).toBe(-50);
  });

  it('identifies the complete C6 credit-card export as c6-bank-export-credit-card-v1', () => {
    const csv =
      'Data de Compra;Nome no Cartão;Final do Cartão;Categoria;Descrição;Parcela;Valor (em US$);Cotação (em R$);Valor (em R$)\n' +
      '07/08/2026;CLIENTE;1234;Supermercado;MERCADO;2/3;0.00;0.00;152.90\n';

    const parsed = parseCsv(csv, { delimiter: ';', decimalSeparator: '.', dateFormat: 'AUTO' });

    expect(parsed.layoutProviderId).toBe('c6-bank-export-credit-card-v1');
    expect(parsed.mapping).toEqual({
      date: 'Data de Compra',
      category: 'Categoria',
      description: 'Descrição',
      installment: 'Parcela',
      value: 'Valor (em R$)',
    });
  });

  it('identifies and parses the Nubank credit-card export as nubank-credit-card-v1', () => {
    const csv =
      'date,title,amount\n' +
      '2026-05-27,Paypal *Cloudflare,"64,17"\n' +
      '2026-05-27,"IOF de ""Paypal *Cloudflare""","2,24"\n' +
      '2026-05-02,Trapezzo Mercantil - Parcela 5/6,"154,90"\n' +
      '2026-05-02,Pagamento recebido,"- 264,14"\n';

    const parsed = parseCsv(csv, { delimiter: ',', decimalSeparator: ',', dateFormat: 'AUTO' });

    expect(parsed.layoutProviderId).toBe('nubank-credit-card-v1');
    expect(parsed.mapping).toEqual({
      date: 'date',
      description: 'title',
      value: 'amount',
    });
    expect(parsed.detectedDateFormat).toBe('YYYY-MM-DD');
    expect(parsed.rows[1]['title']).toBe('IOF de "Paypal *Cloudflare"');
    expect(parseCsvNumber(parsed.rows[0]['amount'], ',')).toBe(64.17);
    expect(parseCsvNumber(parsed.rows[3]['amount'], ',')).toBe(-264.14);
  });

  it('prioritizes and resolves every canonical header', () => {
    const csv =
      'origem;data;descricao;valor;moeda;tipo;categoria;grupo;parcela;beneficiarios;fatura;tags;observacoes;confirmado\n' +
      '019fdb00-a88b-775d-806a-8d74982081ea;2026-08-10;Mercado;-120.50;USD;despesa;Mercado;Casa;Única;a@x.com:50|b@x.com:50;08/2026;casa,comida;Compra;sim\n';
    const parsed = parseCsv(csv, { delimiter: ';', decimalSeparator: '.', dateFormat: 'AUTO' });
    const mapping = parsed.mapping;

    expect(parsed.detectedDateFormat).toBe('YYYY-MM-DD');
    expect(parsed.layoutProviderId).toBe('shared-finances-csv-template-v1');
    expect(mapping).toMatchObject({
      origin: 'origem',
      date: 'data',
      description: 'descricao',
      value: 'valor',
      currency: 'moeda',
      type: 'tipo',
      category: 'categoria',
      group: 'grupo',
      installment: 'parcela',
      beneficiaries: 'beneficiarios',
      bill: 'fatura',
      tags: 'tags',
      observations: 'observacoes',
      confirmed: 'confirmado',
    });
    expect(parseBeneficiaries(parsed.rows[0]['beneficiarios'])).toEqual([
      { email: 'a@x.com', benefitPercent: 50 },
      { email: 'b@x.com', benefitPercent: 50 },
    ]);
  });

  it('resolves every header emitted by the English template', () => {
    const headers = [
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

    const layout = detectCsvLayout(headers);

    expect(layout.providerId).toBe('shared-finances-csv-template-v1');
    expect(layout.mapping).toMatchObject({
      origin: 'origin',
      date: 'date',
      description: 'description',
      value: 'amount',
      currency: 'currency',
      category: 'category',
      group: 'group',
      installment: 'installment',
      beneficiaries: 'beneficiaries',
      bill: 'bill',
      tags: 'tags',
      observations: 'observations',
      confirmed: 'confirmed',
    });
  });

  it('requires manual mapping when no provider matches the headers', () => {
    const layout = detectCsvLayout(['Data da operação', 'Notas', 'Montante líquido']);

    expect(layout.providerId).toBeUndefined();
    expect(layout.mapping).toEqual({});
  });

  it('uses DD/MM for ambiguous dates and validates calendar dates', () => {
    const parsed = parseCsv('data;valor\n01/02/2026;1\n02/03/2026;2\n', {
      delimiter: ';',
      decimalSeparator: '.',
      dateFormat: 'AUTO',
    });
    expect(parsed.detectedDateFormat).toBe('DD/MM/YYYY');
    expect(parseCsvDate('29/02/2025', 'DD/MM/YYYY')).toBeNull();
    expect(parseCsvDate('28/02/2025', 'DD/MM/YYYY')).toBe('2025-02-28');
  });

  it('supports comma decimal values', () => {
    expect(parseCsvNumber('1.234,56', ',')).toBe(1234.56);
  });
});

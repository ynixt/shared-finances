import { describe, expect, it } from 'vitest';

import { buildLocalizedCsvTemplate } from './csv-template';

describe('localized CSV template', () => {
  it('builds a UTF-8 CSV with only the localized header', () => {
    const prefix = 'financesPage.transactionsPage.importPage.csvTemplate';
    const translations: Record<string, string> = {
      [`${prefix}.fileName`]: 'statement-import-template.csv',
      [`${prefix}.columns.origin`]: 'origin',
      [`${prefix}.columns.date`]: 'date',
      [`${prefix}.columns.description`]: 'description',
      [`${prefix}.columns.value`]: 'amount',
      [`${prefix}.columns.currency`]: 'currency',
      [`${prefix}.columns.category`]: 'category',
      [`${prefix}.columns.group`]: 'group',
      [`${prefix}.columns.installment`]: 'installment',
      [`${prefix}.columns.beneficiaries`]: 'beneficiaries',
      [`${prefix}.columns.bill`]: 'bill',
      [`${prefix}.columns.tags`]: 'tags',
      [`${prefix}.columns.observations`]: 'observations',
      [`${prefix}.columns.confirmed`]: 'confirmed',
    };

    const template = buildLocalizedCsvTemplate(key => translations[key]);

    expect(template.fileName).toBe('statement-import-template.csv');
    expect(template.content).toBe(
      '\uFEFForigin;date;description;amount;currency;category;group;installment;beneficiaries;bill;tags;observations;confirmed\r\n',
    );
  });
});

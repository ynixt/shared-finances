import fs from 'node:fs';
import { describe, expect, it } from 'vitest';
import YAML from 'yaml';

import { buildLocalizedCsvTemplate } from './csv-template';
import { sharedFinancesCsvTemplateV1LayoutProvider } from './layout-providers/shared-finances-csv-template-v1.layout-provider';

describe('localized CSV template', () => {
  it('builds a UTF-8 CSV with only the localized header', () => {
    const prefix = 'financesPage.transactionsPage.importPage.csvTemplate';
    const translations: Record<string, string> = {
      [`${prefix}.fileName`]: 'statement-import-template.csv',
      [`${prefix}.columns.origin`]: 'origin',
      [`${prefix}.columns.originName`]: 'origin_name',
      [`${prefix}.columns.date`]: 'date',
      [`${prefix}.columns.description`]: 'description',
      [`${prefix}.columns.value`]: 'value',
      [`${prefix}.columns.currency`]: 'currency',
      [`${prefix}.columns.category`]: 'category',
      [`${prefix}.columns.categoryName`]: 'category_name',
      [`${prefix}.columns.categoryConceptId`]: 'category_concept_id',
      [`${prefix}.columns.group`]: 'group',
      [`${prefix}.columns.groupName`]: 'group_name',
      [`${prefix}.columns.installment`]: 'installment',
      [`${prefix}.columns.beneficiaries`]: 'beneficiaries',
      [`${prefix}.columns.bill`]: 'bill',
      [`${prefix}.columns.tags`]: 'tags',
      [`${prefix}.columns.observations`]: 'observations',
      [`${prefix}.columns.confirmed`]: 'confirmed',
      [`${prefix}.columns.transactionId`]: 'transaction id',
      [`${prefix}.columns.transferId`]: 'transfer id',
      [`${prefix}.columns.seriesId`]: 'series id',
    };

    const template = buildLocalizedCsvTemplate(key => translations[key]);

    expect(template.fileName).toBe('statement-import-template.csv');
    expect(template.content).toBe(
      '\uFEFForigin;origin_name;date;description;value;currency;category;category_name;category_concept_id;group;group_name;installment;beneficiaries;bill;tags;observations;confirmed;transaction id;transfer id;series id\r\n',
    );
  });

  it.each(['en-US', 'pt-BR'])('keeps the %s bundle aligned with canonical detection', language => {
    const bundle = YAML.parse(fs.readFileSync(`src/i18n/finances/transactions/${language}.yaml`, 'utf8'));
    const columns = bundle.financesPage.transactionsPage.importPage.csvTemplate.columns as Record<string, string>;
    const template = buildLocalizedCsvTemplate(key => {
      const field = key.split('.').at(-1)!;
      return field === 'fileName' ? 'template.csv' : columns[field];
    });
    const headers = template.content
      .replace(/^\uFEFF/, '')
      .trim()
      .split(';');

    expect(sharedFinancesCsvTemplateV1LayoutProvider.matches(headers)).toBe(true);
    expect(Object.keys(sharedFinancesCsvTemplateV1LayoutProvider.detect(headers))).toHaveLength(20);
  });
});

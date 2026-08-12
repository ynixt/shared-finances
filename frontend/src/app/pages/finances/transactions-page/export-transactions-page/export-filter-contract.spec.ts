import fs from 'node:fs';
import { describe, expect, it } from 'vitest';

describe('export filter template contract', () => {
  const template = fs.readFileSync(
    'src/app/pages/finances/transactions-page/export-transactions-page/export-filters.component.html',
    'utf8',
  );

  it('shows the shared all placeholder on every empty filter except group', () => {
    expect(template.match(/\[placeholder\]="'general\.all' \| translate"/g)).toHaveLength(5);
    expect(template).toContain('[placeholder]="\'financesPage.transactionsPage.exportPage.filters.personalScope\' | translate"');
  });

  it('offers transaction types and no payment type filter', () => {
    expect(template).toContain('formControlName="entryTypes"');
    expect(template).not.toContain('formControlName="paymentTypes"');
  });
});

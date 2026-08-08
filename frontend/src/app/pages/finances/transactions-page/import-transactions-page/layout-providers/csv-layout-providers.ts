import { c6BankExportCreditCardV1LayoutProvider } from './c6-bank-export-credit-card-v1.layout-provider';
import type { CsvLayoutDetection, CsvLayoutProvider } from './csv-layout-provider';
import { nubankCreditCardV1LayoutProvider } from './nubank-credit-card-v1.layout-provider';
import { sharedFinancesCsvTemplateV1LayoutProvider } from './shared-finances-csv-template-v1.layout-provider';

export const csvLayoutProviders: readonly CsvLayoutProvider[] = [
  c6BankExportCreditCardV1LayoutProvider,
  nubankCreditCardV1LayoutProvider,
  sharedFinancesCsvTemplateV1LayoutProvider,
];

export function detectCsvLayout(headers: string[]): CsvLayoutDetection {
  const provider = csvLayoutProviders.find(candidate => candidate.matches(headers));
  if (provider == null) {
    return { mapping: {} };
  }
  return {
    providerId: provider.id,
    mapping: provider.detect(headers),
  };
}

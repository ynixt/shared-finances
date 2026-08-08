import { CsvDateFormat } from './csv-statement-parser';
import { MappingOption } from './import-transactions.models';

export const CSV_IMPORT_FIXED_MAPPING_VALUE = '__FIXED_VALUE__';
export const CSV_IMPORT_BILL_FROM_DATE_MAPPING_VALUE = '__BILL_FROM_DATE__';
export const CSV_IMPORT_PREVIEW_PAGE_SIZE_OPTIONS: number[] = [20, 50, 100, 500];

export const CSV_IMPORT_DATE_FORMATS: Array<{ labelKey: string; value: CsvDateFormat }> = [
  { labelKey: 'financesPage.transactionsPage.importPage.csvOptions.dateFormats.auto', value: 'AUTO' },
  { labelKey: 'financesPage.transactionsPage.importPage.csvOptions.dateFormats.dayMonthYear', value: 'DD/MM/YYYY' },
  { labelKey: 'financesPage.transactionsPage.importPage.csvOptions.dateFormats.monthDayYear', value: 'MM/DD/YYYY' },
  { labelKey: 'financesPage.transactionsPage.importPage.csvOptions.dateFormats.iso', value: 'YYYY-MM-DD' },
];

export const CSV_IMPORT_MAPPING_OPTIONS: MappingOption[] = [
  { field: 'origin', labelKey: 'financesPage.transactionsPage.importPage.fields.origin' },
  { field: 'date', labelKey: 'financesPage.transactionsPage.importPage.fields.date' },
  { field: 'description', labelKey: 'financesPage.transactionsPage.importPage.fields.description' },
  { field: 'value', labelKey: 'financesPage.transactionsPage.importPage.fields.value' },
  { field: 'credit', labelKey: 'financesPage.transactionsPage.importPage.fields.credit' },
  { field: 'debit', labelKey: 'financesPage.transactionsPage.importPage.fields.debit' },
  { field: 'category', labelKey: 'financesPage.transactionsPage.importPage.fields.category' },
  { field: 'currency', labelKey: 'financesPage.transactionsPage.importPage.fields.currency' },
  { field: 'beneficiaries', labelKey: 'financesPage.transactionsPage.importPage.fields.beneficiaries' },
  { field: 'transactionId', labelKey: 'financesPage.transactionsPage.importPage.fields.transactionId' },
  { field: 'installment', labelKey: 'financesPage.transactionsPage.importPage.fields.installment' },
  { field: 'group', labelKey: 'financesPage.transactionsPage.importPage.fields.group' },
  { field: 'bill', labelKey: 'financesPage.transactionsPage.importPage.fields.bill' },
  { field: 'tags', labelKey: 'financesPage.transactionsPage.importPage.fields.tags' },
  { field: 'observations', labelKey: 'financesPage.transactionsPage.importPage.fields.observations' },
  { field: 'confirmed', labelKey: 'financesPage.transactionsPage.importPage.fields.confirmed' },
];

import { CsvColumnField } from './csv-statement-parser';

export interface PreviewBeneficiary {
  benefitPercent: number;
  email?: string;
  userId: string;
}

export interface ImportPreviewRow {
  beneficiaries: PreviewBeneficiary[];
  billDate?: string;
  categoryId?: string;
  confirmed: boolean;
  createFollowingInstallments: boolean;
  createPreviousInstallments: boolean;
  convertedValue?: number;
  convertedValueOverridden: boolean;
  conversionError?: string;
  conversionLoading: boolean;
  conversionRate?: number;
  conversionTargetCurrency: string;
  currency?: string;
  currencySource: 'FALLBACK' | 'FILE' | 'MANUAL';
  date?: string;
  duplicate: boolean;
  externalTransactionId?: string;
  transferGroupId?: string;
  seriesGroupId?: string;
  transferDisplayName?: string;
  transferPreviewLeaderIndex?: number;
  previewHidden?: boolean;
  groupId?: string;
  included: boolean;
  index: number;
  installment?: { current: number; total: number };
  name?: string;
  observations?: string;
  parseError?: string;
  raw: Record<string, string>;
  sourceStatementKey?: string;
  tags?: string[];
  value?: number;
  walletItemId?: string;
}

export interface MappingOption {
  field: CsvColumnField;
  labelKey: string;
}

export type FixedValue = boolean | number | string;

export interface ImportBatchRemovedEvent {
  fileHash: string;
  id: string;
}

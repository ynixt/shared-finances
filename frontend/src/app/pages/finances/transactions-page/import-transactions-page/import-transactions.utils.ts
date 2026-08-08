import { ImportPreviewRow, PreviewBeneficiary } from './import-transactions.models';

export function parseImportBill(value: string | undefined): string | undefined {
  const isoMonth = value?.trim().match(/^(\d{4})-(\d{2})$/);
  if (isoMonth != null) {
    const month = Number(isoMonth[2]);
    return month >= 1 && month <= 12 ? `${isoMonth[1]}-${isoMonth[2]}-01` : undefined;
  }
  const match = value?.trim().match(/^(\d{1,2})\/(\d{4})$/);
  if (match == null) return undefined;
  const month = Number(match[1]);
  if (month < 1 || month > 12) return undefined;
  return `${match[2]}-${match[1].padStart(2, '0')}-01`;
}

export function parseImportTags(value: string): string[] {
  return value
    .split(',')
    .map(tag => tag.trim())
    .filter(Boolean);
}

export function formatImportTags(tags: readonly string[] | undefined): string {
  return tags?.join(', ') ?? '';
}

export function convertImportValue(value: number | undefined, rate: number | undefined): number | undefined {
  if (value == null || rate == null) return undefined;
  const converted = value * rate;
  return Math.round((converted + Math.sign(converted) * Number.EPSILON) * 100) / 100;
}

export function formatImportInstallment(installment: ImportPreviewRow['installment']): string {
  return installment == null ? '' : `${installment.current}/${installment.total}`;
}

export function hasValidImportBeneficiaries(groupId: string | undefined, beneficiaries: readonly PreviewBeneficiary[]): boolean {
  if (groupId == null || groupId === '') return true;
  if (beneficiaries.length === 0) return false;
  const userIds = new Set(beneficiaries.map(beneficiary => beneficiary.userId));
  const total = beneficiaries.reduce((sum, beneficiary) => sum + beneficiary.benefitPercent, 0);
  return userIds.size === beneficiaries.length && Math.round(total * 100) / 100 === 100;
}

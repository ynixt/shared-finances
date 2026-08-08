/* eslint-disable */
/* tslint-disable */
import { WalletBeneficiaryLegDto } from '../walletentry/wallet-beneficiary-leg-dto';

export interface ImportLineDto {
  externalTransactionId?: string | null;
  beneficiaries?: Array<WalletBeneficiaryLegDto> | null;
  billDate?: string | null;
  categoryId?: string | null;
  confirmed: boolean;
  createFollowingInstallments: boolean;
  createPreviousInstallments: boolean;
  date: string;
  groupId?: string | null;
  installment?: number | null;
  installmentTotal?: number | null;
  name?: string | null;
  observations?: string | null;
  tags?: Array<string> | null;
  value: number;
  walletItemId: string;
}

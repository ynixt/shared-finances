/* eslint-disable */
/* tslint-disable */
import { PaymentType } from '../../../../domain/enums/payment-type';
import { RecurrenceType } from '../../../../domain/enums/recurrence-type';
import { TransferPurpose } from '../../../../domain/enums/transfer-purpose';
import { WalletEntryType } from '../../../../domain/enums/wallet-entry-type';
import { WalletBeneficiaryLegDto } from './wallet-beneficiary-leg-dto';
import { WalletSourceLegDto } from './wallet-source-leg-dto';

export interface NewEntryDto {
  beneficiaries?: Array<WalletBeneficiaryLegDto> | null;
  categoryId?: string | null;
  confirmed: boolean;
  date: string;
  groupId?: string | null;
  installments?: number | null;
  name?: string | null;
  observations?: string | null;
  originBillDate?: string | null;
  originId?: string | null;
  originValue?: number | null;
  paymentType: PaymentType;
  periodicity?: RecurrenceType | null;
  periodicityQtyLimit?: number | null;
  sources?: Array<WalletSourceLegDto> | null;
  tags?: Array<string> | null;
  targetBillDate?: string | null;
  targetId?: string | null;
  targetValue?: number | null;
  transferPurpose?: TransferPurpose | null;
  type: WalletEntryType;
  value?: number | null;
}

/* eslint-disable */
/* tslint-disable */
import { PaymentType } from '../../../../domain/enums/payment-type';
import { RecurrenceType } from '../../../../domain/enums/recurrence-type';
import { WalletEntryType } from '../../../../domain/enums/wallet-entry-type';
import { WalletSourceLegDto } from './wallet-source-leg-dto';

export interface NewEntryDto {
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
  type: WalletEntryType;
  value?: number | null;
}

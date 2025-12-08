/* eslint-disable */
/* tslint-disable */
import { CreditCardBillStatus } from '../../../../../domain/enums/credit-card-bill-status';

export interface CreditCardBillDto {
  billDate: string;
  closingDate: string;
  creditCardId: string;
  dueDate: string;
  id?: string | null;
  paid: boolean;
  startDate?: string | null;
  status?: CreditCardBillStatus | null;
  value: number;
}

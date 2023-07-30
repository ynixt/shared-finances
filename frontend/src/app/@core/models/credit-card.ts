import { CreditCardBillDate } from "./credit-card-bill-date";

export interface CreditCard {
  id: string;
  name: string;
  closingDay: number;
  paymentDay: number;
  limit: number;
  billDates?: CreditCardBillDate[];
  billDatesValue?: string[];
  availableLimit?: number;
  enabled: boolean;
  displayOnGroup: boolean;
}

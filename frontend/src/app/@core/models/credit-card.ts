export interface CreditCard {
  id: string;
  name: string;
  closingDay: number;
  paymentDay: number;
  limit: number;
  billDates?: string[];
  availableLimit?: number;
  enabled: boolean;
  displayOnGroup: boolean;
}

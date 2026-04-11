/* eslint-disable */
/* tslint-disable */
import { WalletItemDto } from '../wallet-item-dto';

export interface CreditCardDto extends WalletItemDto {
  balance: number;
  daysBetweenDueAndClosing: number;
  dueDay: number;
  dueOnNextBusinessDay: boolean;
  totalLimit: number;
  currency: string;
  enabled: boolean;
  id: string;
  name: string;
  showOnDashboard: boolean;
  userId: string;
}

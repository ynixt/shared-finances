/* eslint-disable */
/* tslint-disable */
import { WalletItemDto } from '../wallet-item-dto';

export interface BankAccountDto extends WalletItemDto {
  balance: number;
  currency: string;
  enabled: boolean;
  id: string;
  name: string;
  showOnDashboard: boolean;
  userId: string;
}

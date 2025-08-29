/* eslint-disable */
/* tslint-disable */
import { WalletItemDto } from '../wallet-item-dto';

export interface BankAccountDto extends WalletItemDto {
  balance: number;
  enabled: boolean;
  id: string;
  name: string;
  userId: string;
}

import { Injectable } from '@nestjs/common';
import { BankAccount } from '../models';
import { NewBankAccountArgs } from '../models/args';
import { BankAccountRepository } from './bank-account.repository';

@Injectable()
export class BankAccountService {
  constructor(private bankAccountRepository: BankAccountRepository) {}

  create(userId: string, creditCard: NewBankAccountArgs): Promise<BankAccount> {
    return this.bankAccountRepository.create(userId, creditCard);
  }
}

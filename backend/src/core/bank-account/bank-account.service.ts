import { Injectable } from '@nestjs/common';
import { AuthenticationError } from 'apollo-server-errors';
import { BankAccount } from '../models';
import { NewBankAccountArgs } from '../models/args';
import { BankAccountRepository } from './bank-account.repository';

@Injectable()
export class BankAccountService {
  constructor(private bankAccountRepository: BankAccountRepository) {}

  create(userId: string, newBankAccount: NewBankAccountArgs): Promise<BankAccount> {
    return this.bankAccountRepository.create({ ...newBankAccount, userId });
  }

  findAllWithUserId(userId: string): Promise<BankAccount[]> {
    return this.bankAccountRepository.findAllWithUserId(userId);
  }

  async changeName(userId: string, bankAccountId: string, newName: string): Promise<BankAccount> {
    const updatedBank = await this.bankAccountRepository.changeName(userId, bankAccountId, newName);

    if (updatedBank == null) {
      throw new AuthenticationError('');
    }

    return updatedBank;
  }

  delete(userId: string, bankAccountId: string): Promise<boolean> {
    return this.bankAccountRepository.delete(userId, bankAccountId);
  }

  existsWithUserId(userId: string, bankAccountId: string): Promise<boolean> {
    return this.bankAccountRepository.existsWithUserId(userId, bankAccountId);
  }
}

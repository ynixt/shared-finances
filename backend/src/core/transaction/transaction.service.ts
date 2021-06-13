import { Injectable } from '@nestjs/common';
import { AuthenticationError } from 'apollo-server-errors';
import { BankAccountService } from '../bank-account';
import { Transaction } from '../models';
import { NewTransactionArgs } from '../models/args';
import { TransactionRepository } from './transaction.repository';

@Injectable()
export class TransactionService {
  constructor(private transacationRepository: TransactionRepository, private bankAccountService: BankAccountService) {}

  async create(userId: string, input: NewTransactionArgs): Promise<Transaction> {
    if (!(await this.bankAccountService.existsWithUserId(userId, input.bankAccountId))) {
      throw new AuthenticationError('');
    }

    return this.transacationRepository.create(input);
  }
}

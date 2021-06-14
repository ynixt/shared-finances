import { forwardRef, Inject, Injectable } from '@nestjs/common';
import { AuthenticationError } from 'apollo-server-errors';
import { BankAccountService } from '../bank-account';
import { CreditCardService } from '../credit-card';
import { MongoRepositoryOptions } from '../data/mongo-repository';
import { Transaction, TransactionType } from '../models';
import { NewTransactionArgs } from '../models/args';
import { TransactionRepository } from './transaction.repository';

@Injectable()
export class TransactionService {
  constructor(
    private transacationRepository: TransactionRepository,
    @Inject(forwardRef(() => BankAccountService))
    private bankAccountService: BankAccountService,
    private creditCardService: CreditCardService,
  ) {}

  async create(userId: string, input: NewTransactionArgs): Promise<Transaction> {
    if (input.bankAccountId != null && !(await this.bankAccountService.existsWithUserId(userId, input.bankAccountId))) {
      throw new AuthenticationError('');
    }

    if (input.bankAccount2Id != null && !(await this.bankAccountService.existsWithUserId(userId, input.bankAccount2Id))) {
      throw new AuthenticationError('');
    }

    if (input.creditCardId != null && !(await this.creditCardService.existsWithUserId(userId, input.creditCardId))) {
      throw new AuthenticationError('');
    }

    const bankAccount2Id = input.bankAccount2Id;
    delete input.bankAccount2Id;

    if (input.transactionType === TransactionType.Transfer) {
      input.value *= -1;

      return this.transacationRepository.runInsideTransaction(async opts => {
        await this.transacationRepository.create({ ...input, bankAccountId: bankAccount2Id, value: input.value * -1 }, opts);
        return this.transacationRepository.create(input, opts);
      });
    } else {
      return this.transacationRepository.create(input);
    }
  }

  async deleteByBankAccountId(userId: string, bankAccountId: string, opts?: MongoRepositoryOptions): Promise<void> {
    if (!(await this.bankAccountService.existsWithUserId(userId, bankAccountId))) {
      throw new AuthenticationError('');
    }

    return this.transacationRepository.deleteByBankAccountId(bankAccountId, opts);
  }
}

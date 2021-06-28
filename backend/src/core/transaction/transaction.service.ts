import { forwardRef, Inject, Injectable } from '@nestjs/common';
import { AuthenticationError } from 'apollo-server-errors';
import * as moment from 'moment';
import { Pagination } from 'src/shared';
import { FBUser } from '../auth/firebase-strategy';
import { BankAccountService } from '../bank-account';
import { CreditCardService } from '../credit-card';
import { MongoRepositoryOptions } from '../data/mongo-repository';
import { GroupService } from '../group';
import { Transaction, TransactionType, TransactionsPage } from '../models';
import { NewTransactionArgs } from '../models/args';
import { TransactionRepository } from './transaction.repository';

@Injectable()
export class TransactionService {
  constructor(
    private transacationRepository: TransactionRepository,
    @Inject(forwardRef(() => BankAccountService))
    private bankAccountService: BankAccountService,
    private creditCardService: CreditCardService,
    private groupService: GroupService,
  ) {}

  async create(user: FBUser, input: NewTransactionArgs): Promise<Transaction> {
    await this.validPermissions(user, input);

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

  async findAll(
    user: FBUser,
    args: { bankAccountId?: string; maxDate?: string; minDate?: string },
    pagination?: Pagination,
  ): Promise<TransactionsPage>;

  async findAll(
    user: FBUser,
    args: { bankAccountId: string; maxDate?: string; minDate?: string },
    pagination: Pagination,
  ): Promise<TransactionsPage> {
    return this.transacationRepository.getByBankAccountId(args.bankAccountId, args, pagination);
  }

  async getBalanceByBankAccount(user: FBUser, bankAccountId: string, args: { maxDate?: string } = {}): Promise<number> {
    const bankAccount = await this.bankAccountService.findById(user.id, bankAccountId);

    args.maxDate ??= moment.utc().toISOString();

    if (bankAccount != null) {
      return this.transacationRepository.getBalanceByBankAccountId(bankAccountId, args);
    }

    return null;
  }

  async getBalanceByBankAccountWithoutCheckPermission(bankAccountId: string, args: { maxDate?: string } = {}): Promise<number> {
    args.maxDate ??= moment.utc().endOf('month').toISOString();

    return this.transacationRepository.getBalanceByBankAccountId(bankAccountId, args);
  }

  private async validPermissions(user: FBUser, input: NewTransactionArgs) {
    // Must be a group of the logged user
    if (input.groupId != null) {
      if (user.groupsId.includes(input.groupId) === false) {
        throw new AuthenticationError('');
      }
    }

    // User one must be the logged user or an user that can access the group of the logged user
    if (input.firstUserId !== user.id && !(await this.groupService.userHasAccessToGroup(input.firstUserId, input.groupId))) {
      throw new AuthenticationError('');
    }

    // User two (if there is) must be access to group of the logged user
    if (input.secondUserId != null && !(await this.groupService.userHasAccessToGroup(input.secondUserId, input.groupId))) {
      throw new AuthenticationError('');
    }

    // Bank account one (if there is) must belongs to first user
    if (input.bankAccountId != null && !(await this.bankAccountService.existsWithUserId(input.firstUserId, input.bankAccountId))) {
      throw new AuthenticationError('');
    }

    // Bank account two (if there is) must belongs to first user
    if (input.bankAccount2Id != null && !(await this.bankAccountService.existsWithUserId(input.secondUserId, input.bankAccount2Id))) {
      throw new AuthenticationError('');
    }

    // Credit card (if there is) must belongs to first user
    if (input.creditCardId != null && !(await this.creditCardService.existsWithUserId(input.firstUserId, input.creditCardId))) {
      throw new AuthenticationError('');
    }
  }
}

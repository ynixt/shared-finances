import { forwardRef, Inject, Injectable } from '@nestjs/common';
import { AuthenticationError } from 'apollo-server-errors';
import { UserInputError } from 'apollo-server-express';
import * as moment from 'moment';
import { Pagination } from 'src/shared';
import { FBUser } from '../auth/firebase-strategy';
import { BankAccountService } from '../bank-account';
import { CreditCardService } from '../credit-card';
import { MongoRepositoryOptions } from '../data/mongo-repository';
import { GroupService } from '../group';
import { Transaction, TransactionType, TransactionsPage, CreditCardSummary } from '../models';
import { BillPaymentCreditCardArgs, EditTransactionArgs, NewTransactionArgs } from '../models/args';
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
        await this.transacationRepository.create(
          { ...input, bankAccountId: bankAccount2Id, userId: input.secondUserId, value: input.value * -1 },
          opts,
        );
        return this.transacationRepository.create({ ...input, userId: input.firstUserId }, opts);
      });
    } else {
      return this.transacationRepository.create({ ...input, userId: input.firstUserId });
    }
  }

  async payCreditCardBill(user: FBUser, input: BillPaymentCreditCardArgs): Promise<Transaction> {
    await this.validPermissionsForPayCreditCardBill(user, input);

    return this.transacationRepository.create({ ...input, userId: user.id, transactionType: TransactionType.CreditCardBillPayment });
  }

  async edit(user: FBUser, input: EditTransactionArgs): Promise<Transaction> {
    const transaction = await this.getById(input.transactionId);

    if (await this.validPermissionsOnExistingTransaction(user, transaction)) {
      await this.validPermissions(user, input);

      if (input.transactionType === TransactionType.Transfer || transaction.transactionType === TransactionType.Transfer) {
        throw new UserInputError('For now is not possible to edit a transfer transaction.');
      } else {
        return this.transacationRepository.edit(input);
      }
    }
    throw new AuthenticationError('');
  }

  getById(transactionId: string, opts?: MongoRepositoryOptions): Promise<Transaction> {
    return this.transacationRepository.getById(transactionId, opts);
  }

  async deleteById(user: FBUser, transactionId: string, opts?: MongoRepositoryOptions): Promise<Transaction> {
    const transaction = await this.getById(transactionId);

    if (await this.validPermissionsOnExistingTransaction(user, transaction)) {
      const deleted = await this.transacationRepository.deleteById(transactionId, opts);

      return deleted ? transaction : null;
    }

    throw new AuthenticationError('');
  }

  async deleteByBankAccountId(userId: string, bankAccountId: string, opts?: MongoRepositoryOptions): Promise<void> {
    if (!(await this.bankAccountService.existsWithUserId(userId, bankAccountId))) {
      throw new AuthenticationError('');
    }

    return this.transacationRepository.deleteByBankAccountId(bankAccountId, opts);
  }

  async findAll(
    user: FBUser,
    args: { bankAccountId: string; maxDate?: string; minDate?: string },
    pagination: Pagination,
  ): Promise<TransactionsPage>;

  async findAll(
    user: FBUser,
    args: { creditCardId: string; maxDate?: string; minDate?: string },
    pagination: Pagination,
  ): Promise<TransactionsPage>;

  async findAll(
    user: FBUser,
    args: { bankAccountId?: string; creditCardId?: string; maxDate?: string; minDate?: string },
    pagination?: Pagination,
  ): Promise<TransactionsPage> {
    if (args.bankAccountId != null) {
      return this.transacationRepository.getByBankAccountId(args.bankAccountId, args, pagination);
    }

    if (args.creditCardId != null) {
      return this.transacationRepository.getByCreditCardId(args.creditCardId, args, pagination);
    }
  }

  async getBalanceByBankAccount(user: FBUser, bankAccountId: string, args: { maxDate?: string } = {}): Promise<number> {
    const bankAccount = await this.bankAccountService.findById(user.id, bankAccountId);

    args.maxDate ??= moment.utc().toISOString();

    if (bankAccount != null) {
      return this.transacationRepository.getBalanceByBankAccountId(bankAccountId, args);
    }

    return null;
  }

  async getCreditCardSummary(user: FBUser, creditCardId: string, args: { maxDate?: string } = {}): Promise<CreditCardSummary> {
    const creditCard = await this.creditCardService.getById(user.id, creditCardId);

    args.maxDate ??= moment.utc().toISOString();

    if (creditCard != null) {
      return this.transacationRepository.getCreditCardSummary(creditCardId, args);
    }

    return null;
  }

  async getBalanceByBankAccountWithoutCheckPermission(bankAccountId: string, args: { maxDate?: string } = {}): Promise<number> {
    args.maxDate ??= moment.utc().toISOString();

    return this.transacationRepository.getBalanceByBankAccountId(bankAccountId, args);
  }

  async getCreditCardBillDatesWithoutCheckPermission(creditCardId: string): Promise<string[]> {
    const queryResponse = (await this.transacationRepository.getCreditCardBillDates(creditCardId)) ?? [];

    return queryResponse.map(qr => qr._id.creditCardBillDate).sort((b1, b2) => b1.localeCompare(b2));
  }

  private async validPermissionsOnExistingTransaction(user: FBUser, transaction: Transaction) {
    if (
      (transaction.groupId != null && user.groupsId.includes(transaction.groupId)) ||
      (transaction.bankAccountId != null && (await this.bankAccountService.existsWithUserId(user.id, transaction.bankAccountId))) ||
      (transaction.creditCardId != null && (await this.creditCardService.existsWithUserId(user.id, transaction.creditCardId)))
    ) {
      return true;
    }

    return false;
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

  private async validPermissionsForPayCreditCardBill(user: FBUser, input: BillPaymentCreditCardArgs) {
    // Must be a group of the logged user
    if (input.groupId != null && user.groupsId.includes(input.groupId) === false) {
      throw new AuthenticationError('');
    }

    // Bank account must belongs to first user
    if (input.bankAccountId != null && !(await this.bankAccountService.existsWithUserId(user.id, input.bankAccountId))) {
      throw new AuthenticationError('');
    }

    // Credit card must belongs to first user
    if (input.creditCardId != null && !(await this.creditCardService.existsWithUserId(user.id, input.creditCardId))) {
      throw new AuthenticationError('');
    }
  }
}

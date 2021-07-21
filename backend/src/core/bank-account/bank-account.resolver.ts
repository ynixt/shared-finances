import { UseGuards } from '@nestjs/common';
import { Args, Float, Mutation, Parent, Query, ResolveField, Resolver } from '@nestjs/graphql';
import { ErrorUtilService } from 'src/shared';
import { FBUser } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { BankAccount, BankAccountSummary, Chart } from '../models';
import { NewBankAccountArgs } from '../models/args';
import { TransactionChartService, TransactionService } from '../transaction';
import { BankAccountService } from './bank-account.service';

@Resolver(() => BankAccount)
export class BankAccountResolver {
  constructor(
    private bankAccountService: BankAccountService,
    private transactionService: TransactionService,
    private errorUtilService: ErrorUtilService,
    private transactionChartService: TransactionChartService,
  ) {}

  @Query(() => BankAccount, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async bankAccount(@GqlCurrentUser() user: FBUser, @Args({ name: 'bankAccountId' }) bankAccountId: string) {
    return this.errorUtilService.tryToGetItem(async () => this.bankAccountService.findById(user.id, bankAccountId));
  }

  @Mutation(() => BankAccount)
  @UseGuards(GqlFirebaseAuthGuard)
  newBankAccount(@GqlCurrentUser() user: FBUser, @Args() newBankAccountArgs: NewBankAccountArgs): Promise<BankAccount> {
    return this.bankAccountService.create(user.id, newBankAccountArgs);
  }

  @Mutation(() => BankAccount)
  @UseGuards(GqlFirebaseAuthGuard)
  changeBankAccountName(
    @GqlCurrentUser() user: FBUser,
    @Args({ name: 'bankAccountId' }) bankAccountId: string,
    @Args({ name: 'name' }) name: string,
  ): Promise<BankAccount> {
    return this.bankAccountService.changeName(user.id, bankAccountId, name);
  }

  @Mutation(() => Boolean)
  @UseGuards(GqlFirebaseAuthGuard)
  deleteBankAccount(@GqlCurrentUser() user: FBUser, @Args({ name: 'bankAccountId' }) bankAccountId: string): Promise<boolean> {
    return this.bankAccountService.delete(user.id, bankAccountId);
  }

  @Query(() => BankAccountSummary, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async bankAccountSummary(
    @GqlCurrentUser() user: FBUser,
    @Args({ name: 'bankAccountId', nullable: true }) bankAccountId?: string,
    @Args({ name: 'maxDate', nullable: true }) maxDate?: string,
  ) {
    return this.transactionService.getBankAccountSummary(user, bankAccountId, { maxDate });
  }

  @ResolveField()
  async balance(@Parent() bankAccount: BankAccount) {
    return this.transactionService.getBalanceByBankAccountWithoutCheckPermission(bankAccount.id);
  }

  @Query(() => [Chart], { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async transactionsBankAccountChart(
    @GqlCurrentUser() user: FBUser,
    @Args({ name: 'timezone' }) timezone: string,
    @Args({ name: 'bankAccountId', nullable: true }) bankAccountId?: string,
    @Args({ name: 'maxDate', nullable: true }) maxDate?: string,
    @Args({ name: 'minDate', nullable: true }) minDate?: string,
  ) {
    return this.errorUtilService.tryToGetItem(async () =>
      this.transactionChartService.getChartByBankAccountId(user, bankAccountId, timezone, { minDate, maxDate }),
    );
  }
}

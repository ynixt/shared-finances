import { UseGuards } from '@nestjs/common';
import { Args, Float, Mutation, Parent, Query, ResolveField, Resolver } from '@nestjs/graphql';
import { ErrorUtilService } from 'src/shared';
import { FBUser } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { BankAccount } from '../models';
import { NewBankAccountArgs } from '../models/args';
import { TransactionService } from '../transaction';
import { BankAccountService } from './bank-account.service';

@Resolver(() => BankAccount)
export class BankAccountResolver {
  constructor(
    private bankAccountService: BankAccountService,
    private transactionService: TransactionService,
    private errorUtilService: ErrorUtilService,
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

  @Query(() => Float, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async bankAccountBalance(
    @GqlCurrentUser() user: FBUser,
    @Args({ name: 'bankAccountId' }) bankAccountId: string,
    @Args({ name: 'maxDate', nullable: true }) maxDate?: string,
  ) {
    return this.transactionService.getBalanceByBankAccount(user, bankAccountId, { maxDate });
  }
}

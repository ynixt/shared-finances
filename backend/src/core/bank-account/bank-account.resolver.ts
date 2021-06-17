import { UseGuards } from '@nestjs/common';
import { Args, Mutation, Resolver } from '@nestjs/graphql';
import { FBUser } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { BankAccount } from '../models';
import { NewBankAccountArgs } from '../models/args';
import { BankAccountService } from './bank-account.service';

@Resolver(() => BankAccount)
export class BankAccountResolver {
  constructor(private bankAccountService: BankAccountService) {}

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
}

import { UseGuards } from '@nestjs/common';
import { Args, Mutation, Resolver } from '@nestjs/graphql';
import { FirebaseUserWithId } from '../auth/firebase-strategy';
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
  newBankAccount(@GqlCurrentUser() user: FirebaseUserWithId, @Args() newBankAccountArgs: NewBankAccountArgs): Promise<BankAccount> {
    return this.bankAccountService.create(user.id, newBankAccountArgs);
  }
}

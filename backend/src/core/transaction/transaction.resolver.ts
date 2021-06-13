import { UseGuards } from '@nestjs/common';
import { Args, Mutation, Resolver } from '@nestjs/graphql';
import { FirebaseUserWithId } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { Transaction } from '../models';
import { NewTransactionArgs } from '../models/args';
import { TransactionService } from './transaction.service';

@Resolver(() => Transaction)
export class TransactionResolver {
  constructor(private transactionService: TransactionService) {}

  @Mutation(() => Transaction)
  @UseGuards(GqlFirebaseAuthGuard)
  newTransacation(@GqlCurrentUser() user: FirebaseUserWithId, @Args() newBankAccountArgs: NewTransactionArgs): Promise<Transaction> {
    return this.transactionService.create(user.id, newBankAccountArgs);
  }
}

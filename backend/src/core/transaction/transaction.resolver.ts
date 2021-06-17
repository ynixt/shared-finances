import { UseGuards } from '@nestjs/common';
import { Args, Mutation, Parent, ResolveField, Resolver } from '@nestjs/graphql';
import { FBUser } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { CategoryService } from '../category';
import { Category, Transaction } from '../models';
import { NewTransactionArgs } from '../models/args';
import { TransactionService } from './transaction.service';

@Resolver(() => Transaction)
export class TransactionResolver {
  constructor(private transactionService: TransactionService, private categoryService: CategoryService) {}

  @Mutation(() => Transaction)
  @UseGuards(GqlFirebaseAuthGuard)
  newTransacation(@GqlCurrentUser() user: FBUser, @Args() newTransactionArgs: NewTransactionArgs): Promise<Transaction> {
    return this.transactionService.create(user.id, newTransactionArgs);
  }

  @ResolveField()
  async category(@Parent() category: Category) {
    return this.categoryService.getWithoutCheckPermission(category.id);
  }
}

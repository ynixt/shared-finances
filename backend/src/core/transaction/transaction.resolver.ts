import { UseGuards } from '@nestjs/common';
import { Query, Args, Mutation, Parent, ResolveField, Resolver, Int } from '@nestjs/graphql';
import { ErrorUtilService, INITIAL_PAGE, MAX_PAGE_SIZE, Pagination } from 'src/shared';
import { FBUser } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { CategoryService } from '../category';
import { GroupService } from '../group';
import { Category, Transaction, TransactionsPage } from '../models';
import { EditTransactionArgs, NewTransactionArgs } from '../models/args';
import { TransactionService } from './transaction.service';

@Resolver(() => Transaction)
export class TransactionResolver {
  constructor(
    private transactionService: TransactionService,
    private categoryService: CategoryService,
    private groupService: GroupService,
    private errorUtilService: ErrorUtilService,
  ) {}

  @Query(() => TransactionsPage, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async transactions(
    @GqlCurrentUser() user: FBUser,
    @Args({ name: 'bankAccountId' }) bankAccountId: string,
    @Args({ name: 'page', type: () => Int, nullable: true, defaultValue: INITIAL_PAGE }) page: number,
    @Args({ name: 'pageSize', type: () => Int, nullable: true, defaultValue: MAX_PAGE_SIZE }) pageSize: number,
    @Args({ name: 'maxDate', nullable: true }) maxDate?: string,
    @Args({ name: 'minDate', nullable: true }) minDate?: string,
  ) {
    const pagination = new Pagination({ page, pageSize });

    return this.errorUtilService.tryToGetItem(async () =>
      this.transactionService.findAll(user, { bankAccountId, minDate, maxDate }, pagination),
    );
  }

  @Mutation(() => Transaction)
  @UseGuards(GqlFirebaseAuthGuard)
  newTransaction(@GqlCurrentUser() user: FBUser, @Args() newTransactionArgs: NewTransactionArgs): Promise<Transaction> {
    return this.transactionService.create(user, newTransactionArgs);
  }

  @Mutation(() => Transaction)
  @UseGuards(GqlFirebaseAuthGuard)
  editTransaction(@GqlCurrentUser() user: FBUser, @Args() editTransactionArgs: EditTransactionArgs): Promise<Transaction> {
    return this.transactionService.edit(user, editTransactionArgs);
  }

  @Mutation(() => Boolean)
  @UseGuards(GqlFirebaseAuthGuard)
  deleteTransaction(@GqlCurrentUser() user: FBUser, @Args('transactionId') transactionId: string): Promise<boolean> {
    return this.transactionService.deleteById(user, transactionId);
  }

  @ResolveField()
  async category(@Parent() transaction: Transaction) {
    if (transaction.categoryId) {
      return this.categoryService.getWithoutCheckPermission(transaction.categoryId);
    }

    return null;
  }

  @ResolveField()
  async group(@Parent() category: Category) {
    if (category.groupId) {
      return this.groupService.getGroupWithoutCheckPermission(category.groupId);
    }

    return null;
  }
}

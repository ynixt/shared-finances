import { UseGuards } from '@nestjs/common';
import { Query, Args, Mutation, Parent, ResolveField, Resolver, Int, Subscription } from '@nestjs/graphql';
import { PubSub, UserInputError } from 'apollo-server-express';
import { Types } from 'mongoose';
import { ErrorUtilService, INITIAL_PAGE, MAX_PAGE_SIZE, Pagination } from 'src/shared';
import { FBUser } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { CategoryService } from '../category';
import { GroupService } from '../group';
import { Category, Chart, Transaction, TransactionsPage } from '../models';
import { EditTransactionArgs, NewTransactionArgs } from '../models/args';
import { TransactionChartService } from './transaction-chart.service';
import { TransactionService } from './transaction.service';

const pubSub = new PubSub();

enum TransactionPubTrigger {
  transactionCreated = 'transactionCreated',
  transactionDeleted = 'transactionDeleted',
  transactionUpdated = 'transactionUpdated',
}

@Resolver(() => Transaction)
export class TransactionResolver {
  constructor(
    private transactionService: TransactionService,
    private categoryService: CategoryService,
    private groupService: GroupService,
    private errorUtilService: ErrorUtilService,
    private transactionChartService: TransactionChartService,
  ) {}

  @Query(() => TransactionsPage, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async transactions(
    @GqlCurrentUser() user: FBUser,
    @Args({ name: 'page', type: () => Int, nullable: true, defaultValue: INITIAL_PAGE }) page: number,
    @Args({ name: 'pageSize', type: () => Int, nullable: true, defaultValue: MAX_PAGE_SIZE }) pageSize: number,
    @Args({ name: 'bankAccountId', nullable: true }) bankAccountId?: string,
    @Args({ name: 'creditCardId', nullable: true }) creditCardId?: string,
    @Args({ name: 'maxDate', nullable: true }) maxDate?: string,
    @Args({ name: 'minDate', nullable: true }) minDate?: string,
  ) {
    const pagination = new Pagination({ page, pageSize });

    return this.errorUtilService.tryToGetItem(async () => {
      if (bankAccountId != null) {
        return this.transactionService.findAll(user, { bankAccountId, minDate, maxDate }, pagination);
      } else if (creditCardId != null) {
        return this.transactionService.findAll(user, { creditCardId, minDate, maxDate }, pagination);
      }

      throw new UserInputError('id is missing');
    });
  }

  @Mutation(() => Transaction)
  @UseGuards(GqlFirebaseAuthGuard)
  async newTransaction(@GqlCurrentUser() user: FBUser, @Args() newTransactionArgs: NewTransactionArgs): Promise<Transaction> {
    const transactionCreated = await this.transactionService.create(user, newTransactionArgs);

    if (transactionCreated) {
      const usersDestination = await this.getUsersDestinationForPub(transactionCreated);
      pubSub.publish(TransactionPubTrigger.transactionCreated, { transactionCreated, usersDestination });
    }

    return transactionCreated;
  }

  @Mutation(() => Transaction)
  @UseGuards(GqlFirebaseAuthGuard)
  async editTransaction(@GqlCurrentUser() user: FBUser, @Args() editTransactionArgs: EditTransactionArgs): Promise<Transaction> {
    const transactionUpdated = await this.transactionService.edit(user, editTransactionArgs);

    if (transactionUpdated) {
      const usersDestination = await this.getUsersDestinationForPub(transactionUpdated);
      pubSub.publish(TransactionPubTrigger.transactionUpdated, { transactionUpdated, usersDestination });
    }

    return transactionUpdated;
  }

  @Mutation(() => Boolean)
  @UseGuards(GqlFirebaseAuthGuard)
  async deleteTransaction(@GqlCurrentUser() user: FBUser, @Args('transactionId') transactionId: string): Promise<boolean> {
    const transactionDeleted = await this.transactionService.deleteById(user, transactionId);

    if (transactionDeleted) {
      const usersDestination = await this.getUsersDestinationForPub(transactionDeleted);
      pubSub.publish(TransactionPubTrigger.transactionDeleted, { transactionDeleted, usersDestination });
    }

    return transactionDeleted != null;
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

  @Subscription(() => Transaction, {
    nullable: true,
    filter: async (payload, variables, context): Promise<boolean> => {
      const isToThisUser = payload?.usersDestination?.includes(context.req.user.id);

      if (isToThisUser) {
        if (variables.bankAccountId != null && payload?.transactionCreated?.bankAccountId != null) {
          return isToThisUser && variables.bankAccountId === payload.transactionCreated.bankAccountId.toHexString();
        }
      }

      return isToThisUser;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  transactionCreated(@Args({ name: 'bankAccountId', nullable: true }) bankAccountId?: string) {
    return pubSub.asyncIterator(TransactionPubTrigger.transactionCreated);
  }

  @Subscription(() => Transaction, {
    nullable: true,
    filter: async (payload, variables, context): Promise<boolean> => {
      const isToThisUser = payload?.usersDestination?.includes(context.req.user.id);

      if (isToThisUser) {
        if (variables.bankAccountId != null && payload?.transactionUpdated?.bankAccountId != null) {
          return isToThisUser && variables.bankAccountId === payload.transactionUpdated.bankAccountId.toHexString();
        }
      }

      return isToThisUser;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  transactionUpdated(@Args({ name: 'bankAccountId', nullable: true }) bankAccountId?: string) {
    return pubSub.asyncIterator(TransactionPubTrigger.transactionUpdated);
  }

  @Subscription(() => Transaction, {
    nullable: true,
    filter: async (payload, variables, context): Promise<boolean> => {
      const isToThisUser = payload?.usersDestination?.includes(context.req.user.id);

      if (isToThisUser) {
        if (variables.bankAccountId != null && payload?.transactionDeleted?.bankAccountId != null) {
          return isToThisUser && variables.bankAccountId === payload.transactionDeleted.bankAccountId.toHexString();
        }
      }

      return isToThisUser;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  transactionDeleted(@Args({ name: 'bankAccountId', nullable: true }) bankAccountId?: string) {
    return pubSub.asyncIterator(TransactionPubTrigger.transactionDeleted);
  }

  @Query(() => [Chart], { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async transactionsChart(
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

  private async getUsersDestinationForPub(transaction: Transaction): Promise<string[]> {
    return transaction.userId != null
      ? [(transaction.userId as unknown as Types.ObjectId).toHexString()]
      : await this.groupService.getUsersIdFromGroup(transaction.groupId);
  }
}

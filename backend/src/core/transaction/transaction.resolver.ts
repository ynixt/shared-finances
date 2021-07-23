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
import { Category, Transaction, TransactionsPage } from '../models';
import { BillPaymentCreditCardArgs, EditTransactionArgs, NewTransactionArgs } from '../models/args';
import { TransactionService } from './transaction.service';

const pubSub = new PubSub();

enum TransactionPubTrigger {
  bankAccountTransactionCreated = 'bankAccountTransactionCreated',
  bankAccountTransactionDeleted = 'bankAccountTransactionDeleted',
  bankAccountTransactionUpdated = 'bankAccountTransactionUpdated',
  creditCardTransactionCreated = 'creditCardTransactionCreated',
  creditCardTransactionDeleted = 'creditCardTransactionDeleted',
  creditCardTransactionUpdated = 'creditCardTransactionUpdated',
}

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
    @Args({ name: 'page', type: () => Int, nullable: true, defaultValue: INITIAL_PAGE }) page: number,
    @Args({ name: 'pageSize', type: () => Int, nullable: true, defaultValue: MAX_PAGE_SIZE }) pageSize: number,
    @Args({ name: 'bankAccountId', nullable: true }) bankAccountId?: string,
    @Args({ name: 'creditCardId', nullable: true }) creditCardId?: string,
    @Args({ name: 'groupId', nullable: true }) groupId?: string,
    @Args({ name: 'maxDate', nullable: true }) maxDate?: string,
    @Args({ name: 'minDate', nullable: true }) minDate?: string,
    @Args({ name: 'creditCardBillDate', nullable: true }) creditCardBillDate?: string,
  ) {
    const pagination = new Pagination({ page, pageSize });

    return this.errorUtilService.tryToGetItem(async () => {
      let idsUsed = 0;

      if (groupId != null) {
        idsUsed++;
      }
      if (bankAccountId != null) {
        idsUsed++;
      }
      if (creditCardId != null) {
        idsUsed++;
      }

      if (idsUsed === 3) {
        throw new UserInputError('choose just one id between: groupId, bankAccountId, creditCardId');
      }

      if (bankAccountId != null) {
        return this.transactionService.findAll(user, { bankAccountId, minDate, maxDate }, pagination);
      } else if (creditCardId != null) {
        return this.transactionService.findAll(user, { creditCardId, minDate, maxDate, creditCardBillDate }, pagination);
      } else if (groupId != null) {
        return this.transactionService.findAll(user, { groupId, minDate, maxDate, creditCardBillDate }, pagination);
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

      if (transactionCreated.bankAccountId != null) {
        pubSub.publish(TransactionPubTrigger.bankAccountTransactionCreated, {
          bankAccountTransactionCreated: transactionCreated,
          usersDestination,
        });
      }

      if (transactionCreated.creditCardId != null) {
        pubSub.publish(TransactionPubTrigger.creditCardTransactionCreated, {
          creditCardTransactionCreated: transactionCreated,
          usersDestination,
        });
      }
    }

    return transactionCreated;
  }

  @Mutation(() => Transaction)
  @UseGuards(GqlFirebaseAuthGuard)
  async payCreditCardBill(@GqlCurrentUser() user: FBUser, @Args() billPaymentArgs: BillPaymentCreditCardArgs): Promise<Transaction> {
    const transactionCreated = await this.transactionService.payCreditCardBill(user, billPaymentArgs);

    if (transactionCreated) {
      const usersDestination = await this.getUsersDestinationForPub(transactionCreated);

      pubSub.publish(TransactionPubTrigger.bankAccountTransactionCreated, {
        bankAccountTransactionCreated: transactionCreated,
        usersDestination,
      });

      pubSub.publish(TransactionPubTrigger.creditCardTransactionCreated, {
        creditCardTransactionCreated: transactionCreated,
        usersDestination,
      });
    }

    return transactionCreated;
  }

  @Mutation(() => Transaction)
  @UseGuards(GqlFirebaseAuthGuard)
  async editTransaction(@GqlCurrentUser() user: FBUser, @Args() editTransactionArgs: EditTransactionArgs): Promise<Transaction> {
    const transactionUpdated = await this.transactionService.edit(user, editTransactionArgs);

    if (transactionUpdated) {
      const usersDestination = await this.getUsersDestinationForPub(transactionUpdated);

      if (transactionUpdated.bankAccountId != null) {
        pubSub.publish(TransactionPubTrigger.bankAccountTransactionUpdated, {
          bankAccountTransactionUpdated: transactionUpdated,
          usersDestination,
        });
      }

      if (transactionUpdated.creditCardId != null) {
        pubSub.publish(TransactionPubTrigger.creditCardTransactionUpdated, {
          creditCardTransactionUpdated: transactionUpdated,
          usersDestination,
        });
      }
    }

    return transactionUpdated;
  }

  @Mutation(() => Boolean)
  @UseGuards(GqlFirebaseAuthGuard)
  async deleteTransaction(@GqlCurrentUser() user: FBUser, @Args('transactionId') transactionId: string): Promise<boolean> {
    const transactionDeleted = await this.transactionService.deleteById(user, transactionId);

    if (transactionDeleted) {
      const usersDestination = await this.getUsersDestinationForPub(transactionDeleted);

      if (transactionDeleted.bankAccountId != null) {
        pubSub.publish(TransactionPubTrigger.bankAccountTransactionDeleted, {
          bankAccountTransactionDeleted: transactionDeleted,
          usersDestination,
        });
      }

      if (transactionDeleted.creditCardId != null) {
        pubSub.publish(TransactionPubTrigger.creditCardTransactionDeleted, {
          creditCardTransactionDeleted: transactionDeleted,
          usersDestination,
        });
      }
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

  // begin bank account subscriptions

  @Subscription(() => Transaction, {
    nullable: true,
    filter: async (payload, variables, context): Promise<boolean> => {
      const isToThisUser = payload?.usersDestination?.includes(context.req.user.id);

      if (isToThisUser) {
        if (variables.bankAccountId != null && payload?.bankAccountTransactionCreated?.bankAccountId != null) {
          return isToThisUser && variables.bankAccountId === payload.bankAccountTransactionCreated.bankAccountId.toHexString();
        }
      }

      return isToThisUser;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  bankAccountTransactionCreated(@Args({ name: 'bankAccountId', nullable: true }) bankAccountId?: string) {
    return pubSub.asyncIterator(TransactionPubTrigger.bankAccountTransactionCreated);
  }

  @Subscription(() => Transaction, {
    nullable: true,
    filter: async (payload, variables, context): Promise<boolean> => {
      const isToThisUser = payload?.usersDestination?.includes(context.req.user.id);

      if (isToThisUser) {
        if (variables.bankAccountId != null && payload?.bankAccountTransactionUpdated?.bankAccountId != null) {
          return isToThisUser && variables.bankAccountId === payload.bankAccountTransactionUpdated.bankAccountId.toHexString();
        }
      }

      return isToThisUser;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  bankAccountTransactionUpdated(@Args({ name: 'bankAccountId', nullable: true }) bankAccountId?: string) {
    return pubSub.asyncIterator(TransactionPubTrigger.bankAccountTransactionUpdated);
  }

  @Subscription(() => Transaction, {
    nullable: true,
    filter: async (payload, variables, context): Promise<boolean> => {
      const isToThisUser = payload?.usersDestination?.includes(context.req.user.id);

      if (isToThisUser) {
        if (variables.bankAccountId != null && payload?.bankAccountTransactionDeleted?.bankAccountId != null) {
          return isToThisUser && variables.bankAccountId === payload.bankAccountTransactionDeleted.bankAccountId.toHexString();
        }
      }

      return isToThisUser;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  bankAccountTransactionDeleted(@Args({ name: 'bankAccountId', nullable: true }) bankAccountId?: string) {
    return pubSub.asyncIterator(TransactionPubTrigger.bankAccountTransactionDeleted);
  }

  // end bank account subscriptions

  // begin credit card subscriptions

  @Subscription(() => Transaction, {
    nullable: true,
    filter: async (payload, variables, context): Promise<boolean> => {
      const isToThisUser = payload?.usersDestination?.includes(context.req.user.id);

      if (isToThisUser) {
        if (variables.creditCardId != null && payload?.creditCardTransactionCreated?.creditCardId != null) {
          return isToThisUser && variables.creditCardId === payload.creditCardTransactionCreated.creditCardId.toHexString();
        }
      }

      return isToThisUser;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  creditCardTransactionCreated(@Args({ name: 'creditCardId' }) creditCardId?: string) {
    return pubSub.asyncIterator(TransactionPubTrigger.creditCardTransactionCreated);
  }

  @Subscription(() => Transaction, {
    nullable: true,
    filter: async (payload, variables, context): Promise<boolean> => {
      const isToThisUser = payload?.usersDestination?.includes(context.req.user.id);

      if (isToThisUser) {
        if (variables.creditCardId != null && payload?.creditCardTransactionUpdated?.creditCardId != null) {
          return isToThisUser && variables.creditCardId === payload.creditCardTransactionUpdated.creditCardId.toHexString();
        }
      }

      return isToThisUser;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  creditCardTransactionUpdated(@Args({ name: 'creditCardId' }) creditCardId?: string) {
    return pubSub.asyncIterator(TransactionPubTrigger.creditCardTransactionUpdated);
  }

  @Subscription(() => Transaction, {
    nullable: true,
    filter: async (payload, variables, context): Promise<boolean> => {
      const isToThisUser = payload?.usersDestination?.includes(context.req.user.id);

      if (isToThisUser) {
        if (variables.creditCardId != null && payload?.creditCardTransactionDeleted?.creditCardId != null) {
          return isToThisUser && variables.creditCardId === payload.creditCardTransactionDeleted.creditCardId.toHexString();
        }
      }

      return isToThisUser;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  creditCardTransactionDeleted(@Args({ name: 'creditCardId' }) creditCardId?: string) {
    return pubSub.asyncIterator(TransactionPubTrigger.creditCardTransactionDeleted);
  }

  // end credit card subscriptions

  private async getUsersDestinationForPub(transaction: Transaction): Promise<string[]> {
    return transaction.userId != null
      ? [(transaction.userId as unknown as Types.ObjectId).toHexString()]
      : await this.groupService.getUsersIdFromGroup(transaction.groupId);
  }
}

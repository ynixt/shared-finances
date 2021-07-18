import { forwardRef, Inject, UseGuards } from '@nestjs/common';
import { Args, Mutation, Resolver, Query, Subscription, ResolveField, Parent, Float } from '@nestjs/graphql';
import { PubSub } from 'graphql-subscriptions';
import { ErrorUtilService } from 'src/shared';
import { FBUser } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { Chart, CreditCard, CreditCardSummary } from '../models';
import { EditCreditCardArgs, NewCreditCardArgs } from '../models/args';
import { TransactionChartService, TransactionService } from '../transaction';
import { CreditCardService } from './credit-card.service';

const pubSub = new PubSub();

enum CreditCardPubTrigger {
  creditCardCreated = 'creditCardCreated',
  creditCardDeleted = 'creditCardDeleted',
  creditCardUpdated = 'creditCardUpdated',
}

@Resolver(() => CreditCard)
export class CreditCardResolver {
  constructor(
    private creditCardService: CreditCardService,
    @Inject(forwardRef(() => TransactionService)) private transactionService: TransactionService,
    @Inject(forwardRef(() => TransactionChartService)) private transactionChartService: TransactionChartService,
    private errorUtilService: ErrorUtilService,
  ) {}

  @Mutation(() => CreditCard)
  @UseGuards(GqlFirebaseAuthGuard)
  async newCreditCard(@GqlCurrentUser() user: FBUser, @Args() newCreditCardArgs: NewCreditCardArgs): Promise<CreditCard> {
    const creditCardCreated = await this.creditCardService.create(user.id, newCreditCardArgs);

    if (creditCardCreated) {
      pubSub.publish(CreditCardPubTrigger.creditCardCreated, { creditCardCreated });
    }

    return creditCardCreated;
  }

  @Mutation(() => CreditCard)
  @UseGuards(GqlFirebaseAuthGuard)
  async editCreditCard(@GqlCurrentUser() user: FBUser, @Args() editCreditCardArgs: EditCreditCardArgs): Promise<CreditCard> {
    const creditCardUpdated = await this.creditCardService.edit(user.id, editCreditCardArgs);

    if (creditCardUpdated) {
      pubSub.publish(CreditCardPubTrigger.creditCardUpdated, { creditCardUpdated });
    }

    return creditCardUpdated;
  }

  @Query(() => CreditCard, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  creditCard(@GqlCurrentUser() user: FBUser, @Args({ name: 'creditCardId' }) creditCardId: string): Promise<CreditCard | null> {
    return this.creditCardService.getById(user.id, creditCardId);
  }

  @Query(() => CreditCardSummary, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  creditCardSummary(
    @GqlCurrentUser() user: FBUser,
    @Args({ name: 'creditCardId' }) creditCardId: string,
    @Args({ name: 'maxCreditCardBillDate' }) maxCreditCardBillDate?: string,
  ): Promise<CreditCardSummary | null> {
    return this.transactionService.getCreditCardSummary(user, creditCardId, maxCreditCardBillDate);
  }

  @Query(() => [CreditCard])
  @UseGuards(GqlFirebaseAuthGuard)
  creditCards(@GqlCurrentUser() user: FBUser): Promise<CreditCard[]> {
    return this.creditCardService.findAllWithUserId(user.id);
  }

  @Query(() => Float)
  @UseGuards(GqlFirebaseAuthGuard)
  creditCardAvailableLimit(@GqlCurrentUser() user: FBUser, @Args({ name: 'creditCardId' }) creditCardId: string): Promise<number> {
    return this.transactionService.getCreditCardAvaliableLimit({ user, creditCardId });
  }

  @Query(() => [String])
  @UseGuards(GqlFirebaseAuthGuard)
  creditCardBillDates(@GqlCurrentUser() user: FBUser, @Args({ name: 'creditCardId' }) creditCardId: string): Promise<number> {
    return this.transactionService.getCreditCardBillDates({ user, creditCardId });
  }

  @Mutation(() => Boolean)
  @UseGuards(GqlFirebaseAuthGuard)
  async deleteCreditCard(@GqlCurrentUser() user: FBUser, @Args({ name: 'creditCardId' }) creditCardId: string): Promise<boolean> {
    const creditCardDeleted = await this.creditCardService.delete(user.id, creditCardId);

    if (creditCardDeleted != null) {
      pubSub.publish(CreditCardPubTrigger.creditCardDeleted, { creditCardDeleted });
    }

    return creditCardDeleted != null;
  }

  @Subscription(() => CreditCard, {
    nullable: true,
    filter: async (payload, _, context): Promise<boolean> => {
      return payload?.creditCardCreated?.userId.toHexString() === context.req.user.id;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  creditCardCreated() {
    return pubSub.asyncIterator(CreditCardPubTrigger.creditCardCreated);
  }

  @Subscription(() => CreditCard, {
    nullable: true,
    filter: async (payload, _, context): Promise<boolean> => {
      return payload?.creditCardUpdated?.userId.toHexString() === context.req.user.id;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  creditCardUpdated() {
    return pubSub.asyncIterator(CreditCardPubTrigger.creditCardUpdated);
  }

  @Subscription(() => CreditCard, {
    nullable: true,
    filter: async (payload, _, context): Promise<boolean> => {
      return payload?.creditCardDeleted?.userId.toHexString() === context.req.user.id;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  creditCardDeleted() {
    return pubSub.asyncIterator(CreditCardPubTrigger.creditCardDeleted);
  }

  @ResolveField()
  async billDates(@Parent() creditCard: CreditCard) {
    return this.transactionService.getCreditCardBillDates({ creditCard: creditCard });
  }

  @ResolveField()
  async availableLimit(@Parent() creditCard: CreditCard) {
    return this.transactionService.getCreditCardAvaliableLimit({ creditCard });
  }

  @Query(() => [Chart], { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async transactionsCreditCardChart(
    @GqlCurrentUser() user: FBUser,
    @Args({ name: 'timezone' }) timezone: string,
    @Args({ name: 'creditCardId', nullable: true }) creditCardId?: string,
    @Args({ name: 'maxCreditCardBillDate', nullable: true }) maxCreditCardBillDate?: string,
    @Args({ name: 'minCreditCardBillDate', nullable: true }) minCreditCardBillDate?: string,
  ) {
    return this.errorUtilService.tryToGetItem(async () =>
      this.transactionChartService.getChartByCreditCardId(user, creditCardId, timezone, { minCreditCardBillDate, maxCreditCardBillDate }),
    );
  }
}

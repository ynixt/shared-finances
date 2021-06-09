import { UseGuards } from '@nestjs/common';
import { Args, Mutation, Resolver, Query } from '@nestjs/graphql';
import { FirebaseUserWithId } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { CreditCard } from '../models';
import { EditCreditCardArgs, NewCreditCardArgs } from '../models/args';
import { CreditCardService } from './credit-card.service';

@Resolver(() => CreditCard)
export class CreditCardResolver {
  constructor(private creditCardService: CreditCardService) {}

  @Mutation(() => CreditCard)
  @UseGuards(GqlFirebaseAuthGuard)
  newCreditCard(@GqlCurrentUser() user: FirebaseUserWithId, @Args() newCreditCardArgs: NewCreditCardArgs): Promise<CreditCard> {
    return this.creditCardService.create(user.id, newCreditCardArgs);
  }

  @Mutation(() => CreditCard)
  @UseGuards(GqlFirebaseAuthGuard)
  editCreditCard(@GqlCurrentUser() user: FirebaseUserWithId, @Args() editCreditCardArgs: EditCreditCardArgs): Promise<CreditCard> {
    return this.creditCardService.edit(user.id, editCreditCardArgs);
  }

  @Query(() => CreditCard, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  creditCard(@GqlCurrentUser() user: FirebaseUserWithId, @Args({ name: 'creditCardId' }) creditCardId: string): Promise<CreditCard> {
    return this.creditCardService.getById(user.id, creditCardId);
  }

  @Mutation(() => Boolean)
  @UseGuards(GqlFirebaseAuthGuard)
  deleteCreditCard(@GqlCurrentUser() user: FirebaseUserWithId, @Args({ name: 'creditCardId' }) creditCardId: string): Promise<boolean> {
    return this.creditCardService.delete(user.id, creditCardId);
  }
}

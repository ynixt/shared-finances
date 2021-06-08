import { UseGuards } from '@nestjs/common';
import { Args, Mutation, Resolver } from '@nestjs/graphql';
import { FirebaseUserWithId } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { CreditCard } from '../models';
import { NewCreditCardArgs } from '../models/args';
import { CreditCardService } from './credit-card.service';

@Resolver(() => CreditCard)
export class CreditCardResolver {
  constructor(private creditCardService: CreditCardService) {}

  @Mutation(() => CreditCard)
  @UseGuards(GqlFirebaseAuthGuard)
  newCreditCard(@GqlCurrentUser() user: FirebaseUserWithId, @Args() newCreditCardArgs: NewCreditCardArgs): Promise<CreditCard> {
    return this.creditCardService.create(user.id, newCreditCardArgs);
  }

  @Mutation(() => Boolean)
  @UseGuards(GqlFirebaseAuthGuard)
  deleteCreditCard(@GqlCurrentUser() user: FirebaseUserWithId, @Args({ name: 'creditCardId' }) creditCardId: string): Promise<boolean> {
    return this.creditCardService.delete(user.id, creditCardId);
  }
}

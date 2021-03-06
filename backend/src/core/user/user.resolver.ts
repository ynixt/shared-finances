import { UseGuards } from '@nestjs/common';
import { Parent, Query, ResolveField, Resolver } from '@nestjs/graphql';
import { FBUser } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { BankAccountService } from '../bank-account';
import { CreditCardService } from '../credit-card';
import { User } from '../models';
import { UserService } from './user.service';

@Resolver(() => User)
export class UserResolver {
  constructor(
    private userService: UserService,
    private bankAccountService: BankAccountService,
    private creditCardService: CreditCardService,
  ) {}

  @Query(() => User, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async user(@GqlCurrentUser() user: FBUser) {
    return this.userService.getUserById(user.id);
  }

  @ResolveField()
  async bankAccounts(@Parent() user: User) {
    return this.bankAccountService.findAllWithUserId(user.id);
  }

  @ResolveField()
  async creditCards(@Parent() user: User) {
    return this.creditCardService.findAllWithUserId(user.id);
  }
}

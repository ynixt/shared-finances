import { UseGuards } from '@nestjs/common';
import { Query, Resolver } from '@nestjs/graphql';
import { FirebaseUserWithId } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { User } from '../models';
import { UserService } from './user.service';

@Resolver(() => User)
export class UserResolver {
  constructor(private userService: UserService) {}

  @Query(() => User, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  // async user(@Args('userId') id: string) {
  async user(@GqlCurrentUser() user: FirebaseUserWithId) {
    return this.userService.getUserById(user.id, true);
  }
}

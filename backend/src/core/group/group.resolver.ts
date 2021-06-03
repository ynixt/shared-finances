import { UseGuards } from '@nestjs/common';
import { Args, Mutation, Query, Resolver } from '@nestjs/graphql';
import { FirebaseUserWithId } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { Group } from '../models/group';
import { GroupService } from './group.service';

@Resolver(() => Group)
export class GroupResolver {
  constructor(private groupService: GroupService) {}

  @Query(() => [Group], { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async groups(@GqlCurrentUser() user: FirebaseUserWithId) {
    return this.groupService.getGroupsByUserId(user.id);
  }

  @Mutation(() => String)
  @UseGuards(GqlFirebaseAuthGuard)
  async createShareUrl(@GqlCurrentUser() user: FirebaseUserWithId, @Args({ name: 'groupId' }) groupId: string) {
    return this.groupService.generateShareUrl(user.id, groupId);
  }
}

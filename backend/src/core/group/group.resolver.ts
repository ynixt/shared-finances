import { UseGuards } from '@nestjs/common';
import { Query, Resolver } from '@nestjs/graphql';
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
}

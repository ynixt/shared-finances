import { UseGuards } from '@nestjs/common';
import { Args, Mutation, Parent, Query, ResolveField, Resolver, Subscription } from '@nestjs/graphql';
import { PubSub } from 'graphql-subscriptions';
import { ErrorUtilService } from 'src/shared';
import { FBUser } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { UpdateGroupArgs } from '../models/args';
import { Group } from '../models/group';
import { UserService } from '../user';
import { GroupService } from './group.service';

const pubSub = new PubSub();

enum GroupPubTrigger {
  groupUpdated = 'groupUpdated',
}

@Resolver(() => Group)
export class GroupResolver {
  constructor(private groupService: GroupService, private errorUtilService: ErrorUtilService, private userService: UserService) {}

  @Query(() => [Group], { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async groups(@GqlCurrentUser() user: FBUser) {
    return this.groupService.getGroupsByUserId(user.id);
  }

  @Query(() => Group, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async group(@GqlCurrentUser() user: FBUser, @Args({ name: 'groupId' }) groupId: string) {
    return this.errorUtilService.tryToGetItem(
      async () => {
        return await this.groupService.getGroup(user.id, groupId);
      },
      () => null,
    );
  }

  @Mutation(() => String)
  @UseGuards(GqlFirebaseAuthGuard)
  async createInvite(@GqlCurrentUser() user: FBUser, @Args({ name: 'groupId' }) groupId: string) {
    return this.groupService.generateInvite(user.id, groupId);
  }

  @Mutation(() => String, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async useInvite(@GqlCurrentUser() user: FBUser, @Args({ name: 'invite' }) inviteId: string) {
    return this.groupService.useInvite(user.id, inviteId);
  }

  @Mutation(() => Group, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async updateGroup(@GqlCurrentUser() user: FBUser, @Args() group: UpdateGroupArgs) {
    const groupUpdated = await this.groupService.updateGroup(user.id, group);

    if (groupUpdated) {
      pubSub.publish(GroupPubTrigger.groupUpdated, { groupUpdated: groupUpdated });
    }

    return groupUpdated;
  }

  @Subscription(() => Group, {
    nullable: true,
    filter: (payload, variables, context) => context.req.user.groupsId.includes(variables.groupId),
  })
  @UseGuards(GqlFirebaseAuthGuard)
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  groupUpdated(@Args({ name: 'groupId' }) groupId: string) {
    return pubSub.asyncIterator(GroupPubTrigger.groupUpdated);
  }

  @ResolveField()
  async users(@Parent() group: Group) {
    return this.userService.getByGroupWithoutCheckPermission(group.id);
  }
}

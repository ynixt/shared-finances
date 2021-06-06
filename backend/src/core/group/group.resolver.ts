import { UseGuards } from '@nestjs/common';
import { Args, Mutation, Query, Resolver, Subscription } from '@nestjs/graphql';
import { PubSub } from 'graphql-subscriptions';
import { FirebaseUserWithId } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { UpdateGroupArgs } from '../models/args';
import { Group } from '../models/group';
import { GroupService } from './group.service';

const pubSub = new PubSub();

enum GroupPubTrigger {
  groupUpdated = 'groupUpdated',
}

@Resolver(() => Group)
export class GroupResolver {
  self;

  constructor(private groupService: GroupService) {
    this.self = this;
  }

  @Query(() => [Group], { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async groups(@GqlCurrentUser() user: FirebaseUserWithId) {
    return this.groupService.getGroupsByUserId(user.id);
  }

  @Query(() => Group, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async group(@GqlCurrentUser() user: FirebaseUserWithId, @Args({ name: 'groupId' }) groupId: string) {
    return this.groupService.getGroup(user.id, groupId);
  }

  @Mutation(() => String)
  @UseGuards(GqlFirebaseAuthGuard)
  async createInvite(@GqlCurrentUser() user: FirebaseUserWithId, @Args({ name: 'groupId' }) groupId: string) {
    return this.groupService.generateInvite(user.id, groupId);
  }

  @Mutation(() => String, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async useInvite(@GqlCurrentUser() user: FirebaseUserWithId, @Args({ name: 'invite' }) inviteId: string) {
    return this.groupService.useInvite(user.id, inviteId);
  }

  @Mutation(() => Group, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async updateGroup(@GqlCurrentUser() user: FirebaseUserWithId, @Args() group: UpdateGroupArgs) {
    const groupUpdated = await this.groupService.updateGroup(user.id, group);

    if (groupUpdated) {
      pubSub.publish(GroupPubTrigger.groupUpdated, { groupUpdated: groupUpdated });
    }

    return groupUpdated;
  }

  @Subscription(() => Group, {
    nullable: true,
    filter: async (payload, variables, context): Promise<boolean> => {
      return (
        payload?.groupUpdated?.id === variables.groupId &&
        (await GroupService.instance.userHasAccessToGroup(context.req.user.id, variables.groupId))
      );
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  groupUpdated(@Args({ name: 'groupId' }) groupId: string) {
    return pubSub.asyncIterator(GroupPubTrigger.groupUpdated);
  }
}

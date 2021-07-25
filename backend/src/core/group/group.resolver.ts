import { forwardRef, Inject, UseGuards } from '@nestjs/common';
import { Args, Mutation, Parent, Query, ResolveField, Resolver } from '@nestjs/graphql';
import { ErrorUtilService } from 'src/shared';
import { FBUser } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { Chart, GroupSummary } from '../models';
import { NewGroupArgs, UpdateGroupArgs } from '../models/args';
import { Group } from '../models/group';
import { TransactionChartService, TransactionService } from '../transaction';
import { UserService } from '../user';
import { GroupService } from './group.service';

@Resolver(() => Group)
export class GroupResolver {
  constructor(
    private groupService: GroupService,
    private errorUtilService: ErrorUtilService,
    private userService: UserService,
    @Inject(forwardRef(() => TransactionService)) private transactionService: TransactionService,
    @Inject(forwardRef(() => TransactionChartService)) private transactionChartService: TransactionChartService,
  ) {}

  @Query(() => [Group], { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async groups(@GqlCurrentUser() user: FBUser) {
    return this.groupService.getGroupsByUserId(user.id);
  }

  @Query(() => Group, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async group(@GqlCurrentUser() user: FBUser, @Args({ name: 'groupId' }) groupId: string) {
    return this.errorUtilService.tryToGetItem(async () => this.groupService.getGroup(user.id, groupId));
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
  async newGroup(@GqlCurrentUser() user: FBUser, @Args() group: NewGroupArgs) {
    const groupUpdated = await this.groupService.newGroup(user, group);

    return groupUpdated;
  }

  @Mutation(() => Group, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async updateGroup(@GqlCurrentUser() user: FBUser, @Args() group: UpdateGroupArgs) {
    const groupUpdated = await this.groupService.updateGroup(user.id, group);

    return groupUpdated;
  }

  @Query(() => GroupSummary)
  @UseGuards(GqlFirebaseAuthGuard)
  async groupSummary(
    @GqlCurrentUser() user: FBUser,
    @Args({ name: 'groupId' }) groupId: string,
    @Args({ name: 'minDate' }) minDate: string,
    @Args({ name: 'maxDate' }) maxDate: string,
  ) {
    return this.errorUtilService.tryToGetItem(async () => this.transactionService.getGroupSummary(user, groupId, minDate, maxDate));
  }

  @Query(() => [Chart], { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  async transactionsGroupChart(
    @GqlCurrentUser() user: FBUser,
    @Args({ name: 'timezone' }) timezone: string,
    @Args({ name: 'groupId', nullable: true }) groupId?: string,
    @Args({ name: 'maxDate', nullable: true }) maxDate?: string,
    @Args({ name: 'minDate', nullable: true }) minDate?: string,
  ) {
    return this.errorUtilService.tryToGetItem(async () =>
      this.transactionChartService.getChartByGroupIdId(user, groupId, timezone, { minDate, maxDate }),
    );
  }

  @ResolveField()
  async users(@Parent() group: Group) {
    return this.userService.getByGroupWithoutCheckPermission(group.id);
  }
}

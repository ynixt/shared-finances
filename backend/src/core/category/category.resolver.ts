import { UseGuards } from '@nestjs/common';
import { Args, Mutation, Parent, Query, ResolveField, Resolver, Subscription } from '@nestjs/graphql';
import { PubSub } from 'graphql-subscriptions';
import { ErrorUtilService } from 'src/shared';
import { FBUser } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { GroupService } from '../group';
import { Category } from '../models';
import { EditCategoryArgs, NewCategoryArgs, NewGroupCategoryArgs } from '../models/args';
import { CategoryService } from './category.service';

const pubSub = new PubSub();

enum CategoryPubTrigger {
  userCategoryCreated = 'userCategoryCreated',
  userCategoryDeleted = 'userCategoryDeleted',
  userCategoryUpdated = 'userCategoryUpdated',

  groupCategoryCreated = 'groupCategoryCreated',
  groupCategoryDeleted = 'groupCategoryDeleted',
  groupCategoryUpdated = 'groupCategoryUpdated',
}

@Resolver(() => Category)
export class CategoryResolver {
  constructor(private categoryService: CategoryService, private groupService: GroupService, private errorUtilService: ErrorUtilService) {}

  @Query(() => [Category])
  @UseGuards(GqlFirebaseAuthGuard)
  categories(@GqlCurrentUser() user: FBUser, @Args('groupId', { nullable: true }) groupId?: string): Promise<Category[]> {
    return this.errorUtilService.tryToGetItem(() => {
      if (groupId != null) {
        return this.categoryService.findAllWithGroupId(user.id, groupId);
      } else {
        return this.categoryService.findAllWithUserId(user.id);
      }
    });
  }

  @Query(() => Category, { nullable: true })
  @UseGuards(GqlFirebaseAuthGuard)
  category(
    @GqlCurrentUser() user: FBUser,
    @Args('categoryId') categoryId: string,
    @Args('groupId', { nullable: true }) groupId?: string,
  ): Promise<Category> {
    return this.errorUtilService.tryToGetItem(() => {
      return this.categoryService.getById({
        groupId,
        categoryId,
        loggedUserId: user.id,
      });
    });
  }

  @Mutation(() => Category)
  @UseGuards(GqlFirebaseAuthGuard)
  async newCategory(@GqlCurrentUser() user: FBUser, @Args() newCategoryArgs: NewCategoryArgs): Promise<Category> {
    const categoryCreated = await this.categoryService.create(user.id, newCategoryArgs);

    if (categoryCreated) {
      pubSub.publish(CategoryPubTrigger.userCategoryCreated, { userCategoryCreated: categoryCreated });
    }

    return categoryCreated;
  }

  @Mutation(() => Category)
  @UseGuards(GqlFirebaseAuthGuard)
  async newGroupCategory(@GqlCurrentUser() user: FBUser, @Args() newCategoryArgs: NewGroupCategoryArgs): Promise<Category> {
    const categoryCreated = await this.categoryService.createFromGroup(user.id, newCategoryArgs);

    if (categoryCreated) {
      pubSub.publish(CategoryPubTrigger.groupCategoryCreated, { groupCategoryCreated: categoryCreated });
    }

    return categoryCreated;
  }

  @Mutation(() => Category)
  @UseGuards(GqlFirebaseAuthGuard)
  async editCategory(@GqlCurrentUser() user: FBUser, @Args() editCategoryArgs: EditCategoryArgs): Promise<Category> {
    const userCategoryUpdated = await this.categoryService.update(user.id, editCategoryArgs);

    if (userCategoryUpdated) {
      pubSub.publish(CategoryPubTrigger.userCategoryUpdated, { userCategoryUpdated: userCategoryUpdated });
    }

    return userCategoryUpdated;
  }

  @Mutation(() => Category)
  @UseGuards(GqlFirebaseAuthGuard)
  async editGroupCategory(@GqlCurrentUser() user: FBUser, @Args() editCategoryArgs: EditCategoryArgs): Promise<Category> {
    const groupCategoryUpdated = await this.categoryService.updateFromGroup(user.id, editCategoryArgs);

    if (groupCategoryUpdated) {
      pubSub.publish(CategoryPubTrigger.groupCategoryUpdated, { groupCategoryUpdated: groupCategoryUpdated });
    }

    return groupCategoryUpdated;
  }

  @Mutation(() => Boolean)
  @UseGuards(GqlFirebaseAuthGuard)
  async deleteCategory(@GqlCurrentUser() user: FBUser, @Args({ name: 'categoryId' }) categoryId: string): Promise<boolean> {
    const categoryDeleted = await this.categoryService.delete(user.id, categoryId);

    if (categoryDeleted) {
      pubSub.publish(CategoryPubTrigger.userCategoryDeleted, { userCategoryDeleted: categoryDeleted });
    }

    return categoryDeleted != null;
  }

  @Mutation(() => Boolean)
  @UseGuards(GqlFirebaseAuthGuard)
  async deleteGroupCategory(@GqlCurrentUser() user: FBUser, @Args({ name: 'categoryId' }) categoryId: string): Promise<boolean> {
    const categoryDeleted = await this.categoryService.deleteFromGroup(user.id, categoryId);

    if (categoryDeleted) {
      pubSub.publish(CategoryPubTrigger.groupCategoryDeleted, { groupCategoryDeleted: categoryDeleted });
    }

    return categoryDeleted != null;
  }

  @Subscription(() => Category, {
    nullable: true,
    filter: async (payload, _, context): Promise<boolean> => {
      return payload?.userCategoryCreated?.userId.toHexString() === context.req.user.id;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  userCategoryCreated() {
    return pubSub.asyncIterator(CategoryPubTrigger.userCategoryCreated);
  }

  @Subscription(() => Category, {
    nullable: true,
    filter: async (payload, _, context): Promise<boolean> => {
      return payload?.userCategoryUpdated?.userId.toHexString() === context.req.user.id;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  userCategoryUpdated() {
    return pubSub.asyncIterator(CategoryPubTrigger.userCategoryUpdated);
  }

  @Subscription(() => Category, {
    nullable: true,
    filter: async (payload, _, context): Promise<boolean> => {
      return payload?.userCategoryDeleted?.userId.toHexString() === context.req.user.id;
    },
  })
  @UseGuards(GqlFirebaseAuthGuard)
  userCategoryDeleted() {
    return pubSub.asyncIterator(CategoryPubTrigger.userCategoryDeleted);
  }

  @Subscription(() => Category, {
    nullable: true,
    filter: (payload, _, context) => context.req.user.groupsId.includes(payload?.groupCategoryUpdated?.groupId.toHexString()),
  })
  @UseGuards(GqlFirebaseAuthGuard)
  groupCategoryCreated() {
    return pubSub.asyncIterator(CategoryPubTrigger.groupCategoryCreated);
  }

  @Subscription(() => Category, {
    nullable: true,
    filter: (payload, _, context) => context.req.user.groupsId.includes(payload?.groupCategoryUpdated?.groupId.toHexString()),
  })
  @UseGuards(GqlFirebaseAuthGuard)
  groupCategoryUpdated() {
    return pubSub.asyncIterator(CategoryPubTrigger.groupCategoryUpdated);
  }

  @Subscription(() => Category, {
    nullable: true,
    filter: (payload, _, context) => context.req.user.groupsId.includes(payload?.groupCategoryUpdated?.groupId.toHexString()),
  })
  @UseGuards(GqlFirebaseAuthGuard)
  groupCategoryDeleted() {
    return pubSub.asyncIterator(CategoryPubTrigger.groupCategoryDeleted);
  }

  @ResolveField()
  async group(@Parent() category: Category) {
    if (category.groupId) {
      return this.groupService.getGroupWithoutCheckPermission(category.groupId);
    }

    return null;
  }
}

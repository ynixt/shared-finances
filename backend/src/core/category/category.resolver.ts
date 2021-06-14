import { UseGuards } from '@nestjs/common';
import { Args, Mutation, Query, Resolver, Subscription } from '@nestjs/graphql';
import { PubSub } from 'graphql-subscriptions';
import { FirebaseUserWithId } from '../auth/firebase-strategy';
import { GqlCurrentUser } from '../auth/gql-current-user';
import { GqlFirebaseAuthGuard } from '../auth/gql-firebase-auth-guard';
import { Category } from '../models';
import { EditCategoryArgs, NewCategoryArgs } from '../models/args';
import { CategoryService } from './category.service';

const pubSub = new PubSub();

enum CategoryPubTrigger {
  userCategoryCreated = 'userCategoryCreated',
  userCategoryDeleted = 'userCategoryDeleted',
  userCategoryUpdated = 'userCategoryUpdated',
}

@Resolver(() => Category)
export class CategoryResolver {
  constructor(private categoryService: CategoryService) {}

  @Query(() => [Category])
  @UseGuards(GqlFirebaseAuthGuard)
  async categories(@GqlCurrentUser() user: FirebaseUserWithId) {
    return this.categoryService.findAllWithUserId(user.id);
  }

  @Query(() => Category)
  @UseGuards(GqlFirebaseAuthGuard)
  async category(@GqlCurrentUser() user: FirebaseUserWithId, @Args('categoryId') categoryId: string) {
    return this.categoryService.getById(user.id, categoryId);
  }

  @Mutation(() => Category)
  @UseGuards(GqlFirebaseAuthGuard)
  async newCategory(@GqlCurrentUser() user: FirebaseUserWithId, @Args() newCategoryArgs: NewCategoryArgs): Promise<Category> {
    const categoryCreated = await this.categoryService.create(user.id, newCategoryArgs);

    if (categoryCreated) {
      pubSub.publish(CategoryPubTrigger.userCategoryCreated, { userCategoryCreated: categoryCreated });
    }

    return categoryCreated;
  }

  @Mutation(() => Category)
  @UseGuards(GqlFirebaseAuthGuard)
  async editCategory(@GqlCurrentUser() user: FirebaseUserWithId, @Args() editCategoryArgs: EditCategoryArgs): Promise<Category> {
    const userCategoryUpdated = await this.categoryService.update(user.id, editCategoryArgs);

    if (userCategoryUpdated) {
      pubSub.publish(CategoryPubTrigger.userCategoryUpdated, { userCategoryUpdated: userCategoryUpdated });
    }

    return userCategoryUpdated;
  }

  @Mutation(() => Boolean)
  @UseGuards(GqlFirebaseAuthGuard)
  async deleteCategory(@GqlCurrentUser() user: FirebaseUserWithId, @Args({ name: 'categoryId' }) categoryId: string): Promise<boolean> {
    const categoryDeleted = await this.categoryService.delete(user.id, categoryId);

    if (categoryDeleted) {
      pubSub.publish(CategoryPubTrigger.userCategoryDeleted, { userCategoryDeleted: categoryDeleted });
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
}

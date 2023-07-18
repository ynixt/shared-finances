import { Parent, ResolveField, Resolver } from '@nestjs/graphql';
import { GroupSummaryExpense } from '../models';
import { UserService } from '../user';

@Resolver(() => GroupSummaryExpense)
export class GroupSummaryExpenseResolver {
  constructor(private userService: UserService) {}

  @ResolveField()
  async user(@Parent() groupSummaryExpense: GroupSummaryExpense) {
    return this.userService.getUserById(groupSummaryExpense.userId);
  }
}

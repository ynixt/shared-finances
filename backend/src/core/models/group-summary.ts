import { Field, Float, ObjectType } from '@nestjs/graphql';
import { User } from './user';

export type ExpenseOfUser = { userId: string; value: number };

@ObjectType()
export class GroupSummary {
  constructor(expensesOfUser: ExpenseOfUser[]) {
    if (expensesOfUser?.length > 0) {
      this.totalExpenses = expensesOfUser.reduce((accumulator, expenseOfUser) => accumulator + expenseOfUser.value, 0);

      this.expenses = expensesOfUser.map(
        expenseOfUser =>
          new GroupSummaryExpense(
            expenseOfUser.value,
            expenseOfUser.userId,
            Math.round(((expenseOfUser.value * 100) / this.totalExpenses + Number.EPSILON) * 100) / 100,
          ),
      );
    } else {
      this.totalExpenses = 0;
    }
  }

  @Field(() => [GroupSummaryExpense], { defaultValue: [] })
  expenses: GroupSummaryExpense[];

  @Field(() => Float, { defaultValue: 0 })
  totalExpenses: number;
}

@ObjectType()
export class GroupSummaryExpense {
  constructor(expense: number, userId: string, percentageOfExpenses: number) {
    this.expense = expense;
    this.userId = userId;
    this.percentageOfExpenses = percentageOfExpenses;
  }

  @Field(() => Float)
  expense: number;

  @Field()
  userId: string;

  @Field(() => User)
  user?: User;

  @Field(() => Float)
  percentageOfExpenses: number;
}

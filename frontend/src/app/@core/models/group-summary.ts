import { User } from './user';

export type ExpenseOfUser = { userId: string; value: number };

export interface GroupSummary {
  expenses: GroupSummaryExpense[];
  totalExpenses: number;
}

export interface GroupSummaryExpense {
  expense: number;
  userId: string;
  user?: User;
  percentageOfExpenses: number;
}

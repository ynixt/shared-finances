import { Field, Float, ObjectType } from '@nestjs/graphql';

@ObjectType()
export class CreditCardSummary {
  @Field(() => Float, { defaultValue: 0 })
  bill: number;

  @Field(() => Float, { defaultValue: 0 })
  expenses: number;

  @Field(() => Float, { defaultValue: 0 })
  payments: number;

  @Field(() => Float, { defaultValue: 0 })
  paymentsOfThisBill: number;

  @Field(() => Float, { defaultValue: 0 })
  expensesOfThisBill: number;
}

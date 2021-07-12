import { Field, Float, ObjectType } from '@nestjs/graphql';

@ObjectType()
export class CreditCardSummary {
  @Field(() => Float)
  bill: number;

  @Field(() => Float)
  expenses: number;

  @Field(() => Float)
  payments: number;
}

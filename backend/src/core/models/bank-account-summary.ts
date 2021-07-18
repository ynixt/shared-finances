import { Field, Float, ObjectType } from '@nestjs/graphql';

@ObjectType()
export class BankAccountSummary {
  @Field(() => Float)
  balance: number;

  @Field(() => Float, { nullable: true })
  expenses?: number;

  @Field(() => Float, { nullable: true })
  revenues?: number;
}

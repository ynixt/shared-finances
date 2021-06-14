import { ArgsType, Field, Float } from '@nestjs/graphql';
import { TransactionType } from '../transaction';

@ArgsType()
export class NewTransactionArgs {
  @Field()
  transactionType: TransactionType;

  @Field()
  date: string;

  @Field(() => Float)
  value: number;

  @Field({ nullable: true })
  description?: string;

  @Field()
  bankAccountId: string;
}

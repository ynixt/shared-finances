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

  @Field({ nullable: true })
  bankAccountId: string;

  @Field({ nullable: true })
  creditCardId: string;

  @Field({ nullable: true })
  categoryId?: string;
}

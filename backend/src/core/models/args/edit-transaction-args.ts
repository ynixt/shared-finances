import { ArgsType, Field, Float } from '@nestjs/graphql';
import { TransactionType } from '../transaction';

@ArgsType()
export class EditTransactionArgs {
  @Field()
  transactionId: string;

  @Field()
  transactionType: TransactionType;

  @Field()
  date: string;

  @Field({ nullable: true })
  creditCardBillDate?: string;

  @Field(() => Float)
  value: number;

  @Field({ nullable: true })
  description?: string;

  @Field({ nullable: true })
  bankAccountId: string;

  @Field({ nullable: true })
  bankAccount2Id: string;

  @Field({ nullable: true })
  creditCardId: string;

  @Field({ nullable: true })
  categoryId?: string;

  @Field({ nullable: true })
  groupId?: string;

  @Field()
  firstUserId: string;

  @Field({ nullable: true })
  secondUserId: string;
}

import { ArgsType, Field, Float } from '@nestjs/graphql';
import { TransactionType } from '../transaction';

@ArgsType()
export class BillPaymentCreditCardArgs {
  transactionType: TransactionType = TransactionType.CreditCardBillPayment;

  @Field()
  date: string;

  @Field()
  creditCardBillDate: string;

  @Field()
  bankAccountId: string;

  @Field()
  creditCardId: string;

  @Field(() => Float)
  value: number;

  @Field({ nullable: true })
  description?: string;

  @Field({ nullable: true })
  groupId?: string;
}

import { ArgsType, Field, Float, Int } from '@nestjs/graphql';

@ArgsType()
export class EditCreditCardArgs {
  @Field()
  id: string;

  @Field(() => Int)
  closingDay: number;

  @Field(() => Int)
  paymentDay: number;

  @Field()
  name: string;

  @Field(() => Float)
  limit: number;

  @Field(() => Boolean)
  enabled: boolean;

  @Field(() => Boolean)
  displayOnGroup: boolean;
}

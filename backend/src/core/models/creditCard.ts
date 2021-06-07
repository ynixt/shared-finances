import { Field, Float, ID, Int, ObjectType } from '@nestjs/graphql';
import { Schema, Prop } from '@nestjs/mongoose';

@ObjectType()
@Schema()
export class CreditCard {
  @Field(() => ID)
  id: string;

  @Field()
  @Prop({ unique: true })
  name: string;

  @Field(() => Int)
  @Prop()
  closingDay: number;

  @Field(() => Int)
  @Prop()
  paymentDay: number;

  @Field(() => Float)
  @Prop()
  limit: number;
}

import { Field, Float, ID, Int, ObjectType } from '@nestjs/graphql';
import { Schema, Prop, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';

export type CreditCardDocument = CreditCard & Document;

@ObjectType()
@Schema()
export class CreditCard {
  @Field(() => ID)
  id: string;

  @Prop({ type: MongooseSchema.Types.ObjectId, index: true, required: true })
  userId: string;

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

export const CreditCardSchema = SchemaFactory.createForClass(CreditCard);

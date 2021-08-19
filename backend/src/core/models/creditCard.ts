import { Field, Float, ID, Int, ObjectType } from '@nestjs/graphql';
import { Schema, Prop, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';

export type CreditCardDocument = CreditCard & Document;

@ObjectType()
@Schema({ autoCreate: true })
export class CreditCard {
  @Field(() => ID)
  id: string;

  @Prop({ type: MongooseSchema.Types.ObjectId, index: true, required: true })
  userId: string;

  @Field()
  @Prop()
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

  @Field(() => Float)
  availableLimit?: number;

  @Field(() => [String], { nullable: true })
  billDates?: string[];

  @Field(() => Boolean)
  @Prop({ required: true, index: true })
  enabled: boolean;

  @Field(() => Boolean)
  @Prop({ required: true, index: true })
  displayOnGroup: boolean;
}

export const CreditCardSchema = SchemaFactory.createForClass(CreditCard);

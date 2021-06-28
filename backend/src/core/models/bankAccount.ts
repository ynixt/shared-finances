import { Field, Float, ID, ObjectType } from '@nestjs/graphql';
import { Schema, Prop, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';

export type BankAccountDocument = BankAccount & Document;

@ObjectType()
@Schema()
export class BankAccount {
  @Field(() => ID)
  id: string;

  @Field()
  @Prop({ required: true })
  name: string;

  @Prop({ type: MongooseSchema.Types.ObjectId, index: true, required: true })
  userId: string;

  @Field(() => Float, { nullable: true })
  balance?: string;
}

export const BankAccountSchema = SchemaFactory.createForClass(BankAccount);

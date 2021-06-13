import { Field, Float, ID, ObjectType } from '@nestjs/graphql';
import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';

export type TransactionDocument = Transaction & Document;

export enum TransactionType {
  Revenue = 'Revenue',
  Expense = 'Expense',
  Transfer = 'Transfer',
}

@ObjectType()
@Schema()
export class Transaction {
  @Field(() => ID)
  id: string;

  @Field()
  @Prop({ type: MongooseSchema.Types.ObjectId, index: true, required: true })
  bankAccountId: string;

  @Field()
  @Prop({ index: true, required: true })
  transactionType: TransactionType;

  @Field()
  @Prop({ type: MongooseSchema.Types.ObjectId, index: true })
  groupId?: string;

  @Field()
  @Prop({ index: true, required: true })
  date: string;

  @Field(() => Float)
  @Prop({ required: true })
  value: number;

  @Field({ nullable: true })
  @Prop({ maxlength: 50 })
  description?: string;
}

export const TransacationSchema = SchemaFactory.createForClass(Transaction);

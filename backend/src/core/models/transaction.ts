import { Field, Float, ID, ObjectType } from '@nestjs/graphql';
import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';
import { Category } from './category';
import { Group } from './group';

export type TransactionDocument = Transaction & Document;

export enum TransactionType {
  Revenue = 'Revenue',
  Expense = 'Expense',
  Transfer = 'Transfer',
  CreditCard = 'CreditCard',
}

@ObjectType()
@Schema()
export class Transaction {
  @Field(() => ID)
  id: string;

  @Field({ nullable: true })
  @Prop({
    type: MongooseSchema.Types.ObjectId,
    index: true,
    required: function () {
      return this.transactionType !== TransactionType.CreditCard;
    },
  })
  bankAccountId?: string;

  @Field({ nullable: true })
  @Prop({
    type: MongooseSchema.Types.ObjectId,
    index: true,
    required: function () {
      return this.transactionType === TransactionType.CreditCard;
    },
  })
  creditCardId?: string;

  @Field()
  @Prop({ index: true, required: true })
  transactionType: TransactionType;

  @Field({ nullable: true })
  @Prop({ type: MongooseSchema.Types.ObjectId, index: true })
  groupId?: string;

  @Prop({ type: MongooseSchema.Types.ObjectId, index: true })
  userId?: string;

  @Field(() => Group, { nullable: true })
  group?: Group;

  @Field()
  @Prop({ index: true, required: true })
  date: string;

  @Field(() => Float)
  @Prop({ required: true })
  value: number;

  @Field({ nullable: true })
  @Prop({ maxlength: 50 })
  description?: string;

  @Field({ nullable: true })
  @Prop()
  categoryId?: string;

  @Field(() => Category, { nullable: true })
  category?: Category;
}

export const TransacationSchema = SchemaFactory.createForClass(Transaction);

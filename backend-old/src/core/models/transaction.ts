import { Field, Float, ID, Int, ObjectType } from '@nestjs/graphql';
import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';
import { Category } from './category';
import { Group } from './group';
import { User } from './user';

export type TransactionDocument = Transaction & Document;

export enum TransactionType {
  Revenue = 'Revenue',
  Expense = 'Expense',
  Transfer = 'Transfer',
  CreditCard = 'CreditCard',
  CreditCardBillPayment = 'CreditCardBillPayment',
}

@ObjectType()
@Schema({ autoCreate: true })
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
      return this.transactionType === TransactionType.CreditCard || this.transactionType === TransactionType.CreditCardBillPayment;
    },
  })
  creditCardId?: string;

  @Field()
  @Prop({ index: true, required: true })
  transactionType: TransactionType;

  @Field({ nullable: true })
  @Prop({ type: MongooseSchema.Types.ObjectId, index: true })
  groupId?: string;

  @Prop({ type: MongooseSchema.Types.ObjectId, index: true, ref: 'User' })
  userId: string;

  @Field({ nullable: true })
  // @Prop({ virtual: true, localField: 'userId', ref: 'User', foreignField: '_id', justOne: true })
  user?: User;

  @Field(() => Group, { nullable: true })
  group?: Group;

  @Field()
  @Prop({ index: true, required: true })
  date: string;

  @Field({ nullable: true })
  @Prop({
    index: true,
    required: function () {
      return this.transactionType === TransactionType.CreditCard;
    },
  })
  creditCardBillDate?: string;

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

  @Field({ nullable: true })
  @Prop({ index: true })
  installmentId?: string;

  @Field(() => Int, { nullable: true })
  @Prop({
    required: function () {
      return this.installmentId != null;
    },
  })
  installment?: number;

  @Field(() => Int, { nullable: true })
  @Prop({
    required: function () {
      return this.installmentId != null;
    },
  })
  totalInstallments?: number;
}

export const TransacationSchema = SchemaFactory.createForClass(Transaction);

TransacationSchema.virtual('user', {
  ref: 'User',
  localField: 'userId',
  foreignField: '_id',
  justOne: true,
});

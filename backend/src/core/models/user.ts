import { Field, ID, ObjectType } from '@nestjs/graphql';
import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';

import { BankAccount } from './bankAccount';
import { CreditCard } from './creditCard';
import { Group } from './group';

export type UserDocument = User & Document;

@ObjectType()
@Schema()
export class User {
  @Field(() => ID)
  id: string;

  @Field()
  @Prop({
    index: true,
    unique: true,
  })
  uid: string;

  @Field()
  @Prop({
    index: true,
    unique: true,
  })
  email: string;

  @Field()
  @Prop()
  name: string;

  @Field({ nullable: true })
  @Prop()
  photoURL?: string;

  @Prop([{ type: MongooseSchema.Types.ObjectId, index: true }])
  @Field(() => [String])
  groupsId?: string[];

  @Field(() => Group)
  groups?: Group[];

  @Field(() => [BankAccount], { nullable: true })
  bankAccounts?: BankAccount[];

  @Field(() => [CreditCard], { nullable: true })
  creditCards?: CreditCard[];
}

export const UserSchema = SchemaFactory.createForClass(User);

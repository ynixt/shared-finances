import { Field, ID, ObjectType } from '@nestjs/graphql';
import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';

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

  @Field({ nullable: true })
  public email?: string;

  @Field({ nullable: true })
  public name?: string;

  @Field({ nullable: true })
  public photoURL?: string;

  @Prop([{ type: MongooseSchema.Types.ObjectId, index: true }])
  @Field(() => Group)
  groups?: Group[];

  @Prop()
  @Field(() => [CreditCard], { nullable: true })
  creditCards?: CreditCard[];
}

export const UserSchema = SchemaFactory.createForClass(User);

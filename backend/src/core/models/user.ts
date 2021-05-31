import { Field, ID, ObjectType } from '@nestjs/graphql';
import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import * as mongoose from 'mongoose';

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

  @Prop([{ type: mongoose.Schema.Types.ObjectId }])
  @Field(() => Group)
  groups?: Group[];
}

export const UserSchema = SchemaFactory.createForClass(User);

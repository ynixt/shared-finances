import { Field, ID, ObjectType } from '@nestjs/graphql';
import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import * as mongoose from 'mongoose';

import { User } from './user';

export type GroupDocument = Group & Document;

@ObjectType()
@Schema()
export class Group {
  @Field(() => ID)
  id: string;

  @Field()
  @Prop()
  name: string;

  @Prop([{ type: mongoose.Schema.Types.ObjectId, index: true }])
  @Field(() => User)
  users?: mongoose.Types.Array<User>;
}

export const GroupSchema = SchemaFactory.createForClass(Group);

import { Field, ID, ObjectType } from '@nestjs/graphql';
import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';

import { User } from './user';

export type GroupDocument = Group & Document;

@ObjectType()
@Schema({ autoCreate: true })
export class Group {
  @Field(() => ID)
  id: string;

  @Field()
  @Prop()
  name: string;

  @Prop([{ type: MongooseSchema.Types.ObjectId, index: true }])
  @Field(() => [String], { nullable: true })
  usersId?: string[];

  @Field(() => [User], { nullable: true })
  users?: User[];
}

export const GroupSchema = SchemaFactory.createForClass(Group);

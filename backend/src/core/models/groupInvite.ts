import { Field, ID, ObjectType } from '@nestjs/graphql';
import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import * as mongoose from 'mongoose';
import { Group } from './group';

export type GroupInviteDocument = GroupInvite & Document;

@ObjectType()
@Schema()
export class GroupInvite {
  @Field(() => ID)
  id: string;

  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'Group', index: true })
  group: Group;

  @Prop({ index: true })
  creationDate: string;
}

export const GroupInviteSchema = SchemaFactory.createForClass(GroupInvite);

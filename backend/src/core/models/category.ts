import { Field, ID, ObjectType } from '@nestjs/graphql';
import { Schema, Prop, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';
import { Group } from './group';

export type CategoryDocument = Category & Document;

@ObjectType()
@Schema({ autoCreate: true })
export class Category {
  @Field(() => ID)
  id: string;

  @Field()
  @Prop({ required: true })
  name: string;

  @Field()
  @Prop()
  color: string;

  @Prop({
    type: MongooseSchema.Types.ObjectId,
    index: true,
    required: function () {
      return this.groupId == null;
    },
  })
  userId?: string;

  @Prop({
    type: MongooseSchema.Types.ObjectId,
    index: true,
    required: function () {
      return this.userId == null;
    },
  })
  groupId?: string;

  @Field(() => Group, { nullable: true })
  group?: Group;
}

export const CategorySchema = SchemaFactory.createForClass(Category);

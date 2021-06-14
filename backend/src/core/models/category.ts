import { Field, ID, ObjectType } from '@nestjs/graphql';
import { Schema, Prop, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';

export type CategoryDocument = Category & Document;

@ObjectType()
@Schema()
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
}

export const CategorySchema = SchemaFactory.createForClass(Category);

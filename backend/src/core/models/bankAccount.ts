import { Field, ID, ObjectType } from '@nestjs/graphql';
import { Schema, Prop } from '@nestjs/mongoose';

@ObjectType()
@Schema()
export class BankAccount {
  @Field(() => ID)
  id: string;

  @Field()
  @Prop({ unique: true })
  name: string;
}

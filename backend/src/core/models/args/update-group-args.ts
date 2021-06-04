// TODO: import { MinLength } from 'class-validator';
import { Field, ArgsType } from '@nestjs/graphql';

@ArgsType()
export class UpdateGroupArgs {
  @Field()
  id: string;

  @Field()
  name: string;
}

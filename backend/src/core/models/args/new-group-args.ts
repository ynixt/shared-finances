import { Field, ArgsType } from '@nestjs/graphql';

@ArgsType()
export class NewGroupArgs {
  @Field()
  name: string;
}
